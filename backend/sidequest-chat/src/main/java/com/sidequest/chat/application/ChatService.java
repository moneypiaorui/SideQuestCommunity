package com.sidequest.chat.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.sidequest.chat.infrastructure.ChatMessageDO;
import com.sidequest.chat.infrastructure.ChatRoomDO;
import com.sidequest.chat.infrastructure.ChatRoomMemberDO;
import com.sidequest.chat.infrastructure.feign.IdentityClient;
import com.sidequest.chat.infrastructure.mapper.ChatMessageMapper;
import com.sidequest.chat.infrastructure.mapper.ChatRoomMapper;
import com.sidequest.chat.infrastructure.mapper.ChatRoomMemberMapper;
import com.sidequest.chat.interfaces.dto.ChatRoomMemberVO;
import com.sidequest.chat.interfaces.dto.ChatRoomVO;
import com.sidequest.chat.interfaces.dto.CreateRoomRequest;
import com.sidequest.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatRoomMapper chatRoomMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final ChatRoomMemberMapper chatRoomMemberMapper;
    private final IdentityClient identityClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public List<ChatRoomVO> getUserRooms(Long userId) {
        List<ChatRoomMemberDO> memberships = chatRoomMemberMapper.selectList(new LambdaQueryWrapper<ChatRoomMemberDO>()
                .eq(ChatRoomMemberDO::getUserId, userId));
        
        return memberships.stream().map(m -> {
            ChatRoomDO room = chatRoomMapper.selectById(m.getRoomId());
            ChatRoomVO vo = new ChatRoomVO();
            vo.setId(room.getId());
            vo.setName(room.getName());
            vo.setType(room.getType());
            
            // 获取最后一条消息
            ChatMessageDO lastMsg = chatMessageMapper.selectOne(new LambdaQueryWrapper<ChatMessageDO>()
                    .eq(ChatMessageDO::getRoomId, room.getId())
                    .orderByDesc(ChatMessageDO::getId)
                    .last("LIMIT 1"));
            if (lastMsg != null) {
                vo.setLastMessage(lastMsg.getContent());
                vo.setLastMessageTime(lastMsg.getCreateTime());
            }
            
            // 获取未读数
            Long unreadCount = chatMessageMapper.selectCount(new LambdaQueryWrapper<ChatMessageDO>()
                    .eq(ChatMessageDO::getRoomId, room.getId())
                    .gt(ChatMessageDO::getId, m.getLastReadMessageId()));
            vo.setUnreadCount(unreadCount.intValue());
            
            // 获取对方信息 (1对1)
            if ("PRIVATE".equals(room.getType())) {
                ChatRoomMemberDO otherMember = chatRoomMemberMapper.selectOne(new LambdaQueryWrapper<ChatRoomMemberDO>()
                        .eq(ChatRoomMemberDO::getRoomId, room.getId())
                        .ne(ChatRoomMemberDO::getUserId, userId));
                if (otherMember != null) {
                    try {
                        Result<IdentityClient.UserDTO> userRes = identityClient.getUserById(otherMember.getUserId());
                        if (userRes.getCode() == 200 && userRes.getData() != null) {
                            vo.setRecipientNickname(userRes.getData().getNickname());
                            vo.setRecipientAvatar(userRes.getData().getAvatar());
                        }
                    } catch (Exception ignored) {}
                }
            }
            
            return vo;
        }).collect(Collectors.toList());
    }

    @Transactional
    public ChatRoomVO createRoom(Long creatorId, CreateRoomRequest request) {
        ChatRoomDO room = new ChatRoomDO();
        room.setName(request.getName());
        room.setType((request.getType() == null || request.getType().isBlank()) ? "GROUP" : request.getType());
        room.setCreateTime(LocalDateTime.now());
        chatRoomMapper.insert(room);

        Set<Long> memberIds = new LinkedHashSet<>();
        memberIds.add(creatorId);
        if (request.getMemberIds() != null) {
            memberIds.addAll(request.getMemberIds());
        }

        for (Long memberId : memberIds) {
            ChatRoomMemberDO member = new ChatRoomMemberDO();
            member.setRoomId(room.getId());
            member.setUserId(memberId);
            member.setLastReadMessageId(0L);
            member.setJoinTime(LocalDateTime.now());
            chatRoomMemberMapper.insert(member);
        }

        ChatRoomVO vo = new ChatRoomVO();
        vo.setId(room.getId());
        vo.setName(room.getName());
        vo.setType(room.getType());
        return vo;
    }

    @Transactional
    public ChatMessageDO sendMessage(Long senderId, Long roomId, String content) {
        ChatMessageDO msg = new ChatMessageDO();
        msg.setRoomId(roomId);
        msg.setSenderId(senderId);
        msg.setContent(content);
        msg.setType("TEXT");
        msg.setStatus(0); // UNREAD
        msg.setCreateTime(LocalDateTime.now());
        chatMessageMapper.insert(msg);

        kafkaTemplate.send("chat-message-topic", roomId.toString(), msg);
        return msg;
    }

    public List<ChatMessageDO> getMessages(Long roomId, Long sinceId) {
        LambdaQueryWrapper<ChatMessageDO> query = new LambdaQueryWrapper<ChatMessageDO>()
                .eq(ChatMessageDO::getRoomId, roomId)
                .orderByAsc(ChatMessageDO::getId);
        if (sinceId != null) {
            query.gt(ChatMessageDO::getId, sinceId);
        }
        return chatMessageMapper.selectList(query);
    }

    public List<ChatRoomMemberVO> getRoomMembers(Long roomId) {
        List<ChatRoomMemberDO> members = chatRoomMemberMapper.selectList(new LambdaQueryWrapper<ChatRoomMemberDO>()
                .eq(ChatRoomMemberDO::getRoomId, roomId));
        return members.stream().map(member -> {
            ChatRoomMemberVO vo = new ChatRoomMemberVO();
            vo.setUserId(member.getUserId());
            vo.setJoinTime(member.getJoinTime());
            try {
                Result<IdentityClient.UserDTO> userRes = identityClient.getUserById(member.getUserId());
                if (userRes.getCode() == 200 && userRes.getData() != null) {
                    vo.setNickname(userRes.getData().getNickname());
                    vo.setAvatar(userRes.getData().getAvatar());
                }
            } catch (Exception ignored) {}
            return vo;
        }).collect(Collectors.toList());
    }

    @Transactional
    public void markAsRead(Long roomId, Long userId) {
        ChatMessageDO lastMsg = chatMessageMapper.selectOne(new LambdaQueryWrapper<ChatMessageDO>()
                .eq(ChatMessageDO::getRoomId, roomId)
                .orderByDesc(ChatMessageDO::getId)
                .last("LIMIT 1"));
        if (lastMsg != null) {
            chatRoomMemberMapper.update(null, new LambdaUpdateWrapper<ChatRoomMemberDO>()
                    .eq(ChatRoomMemberDO::getRoomId, roomId)
                    .eq(ChatRoomMemberDO::getUserId, userId)
                    .set(ChatRoomMemberDO::getLastReadMessageId, lastMsg.getId()));
        }
    }

    @Transactional
    public boolean deleteRoom(Long roomId, Long userId) {
        Long memberCount = chatRoomMemberMapper.selectCount(new LambdaQueryWrapper<ChatRoomMemberDO>()
                .eq(ChatRoomMemberDO::getRoomId, roomId)
                .eq(ChatRoomMemberDO::getUserId, userId));
        if (memberCount == null || memberCount == 0) {
            return false;
        }
        chatMessageMapper.delete(new LambdaQueryWrapper<ChatMessageDO>()
                .eq(ChatMessageDO::getRoomId, roomId));
        chatRoomMemberMapper.delete(new LambdaQueryWrapper<ChatRoomMemberDO>()
                .eq(ChatRoomMemberDO::getRoomId, roomId));
        chatRoomMapper.deleteById(roomId);
        return true;
    }

    @Transactional
    public ChatRoomVO findOrCreatePrivateRoom(Long userId, Long recipientId) {
        // 1. 查找是否存在两人的私聊房间
        List<ChatRoomMemberDO> userRooms = chatRoomMemberMapper.selectList(new LambdaQueryWrapper<ChatRoomMemberDO>()
                .eq(ChatRoomMemberDO::getUserId, userId));
        
        for (ChatRoomMemberDO m : userRooms) {
            ChatRoomDO room = chatRoomMapper.selectById(m.getRoomId());
            if ("PRIVATE".equals(room.getType())) {
                ChatRoomMemberDO other = chatRoomMemberMapper.selectOne(new LambdaQueryWrapper<ChatRoomMemberDO>()
                        .eq(ChatRoomMemberDO::getRoomId, m.getRoomId())
                        .eq(ChatRoomMemberDO::getUserId, recipientId));
                if (other != null) {
                    // 找到了
                    ChatRoomVO vo = new ChatRoomVO();
                    vo.setId(room.getId());
                    vo.setName(room.getName());
                    vo.setType(room.getType());
                    return vo;
                }
            }
        }

        // 2. 不存在则创建
        ChatRoomDO room = new ChatRoomDO();
        room.setName("Private Chat");
        room.setType("PRIVATE");
        room.setCreateTime(LocalDateTime.now());
        chatRoomMapper.insert(room);

        // 添加成员
        ChatRoomMemberDO m1 = new ChatRoomMemberDO();
        m1.setRoomId(room.getId());
        m1.setUserId(userId);
        m1.setLastReadMessageId(0L);
        m1.setJoinTime(LocalDateTime.now());
        chatRoomMemberMapper.insert(m1);

        ChatRoomMemberDO m2 = new ChatRoomMemberDO();
        m2.setRoomId(room.getId());
        m2.setUserId(recipientId);
        m2.setLastReadMessageId(0L);
        m2.setJoinTime(LocalDateTime.now());
        chatRoomMemberMapper.insert(m2);

        ChatRoomVO vo = new ChatRoomVO();
        vo.setId(room.getId());
        vo.setName(room.getName());
        vo.setType(room.getType());
        return vo;
    }
}

