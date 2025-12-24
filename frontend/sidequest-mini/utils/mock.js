// utils/mock.js

export const IS_LOGGED_IN = false; // 一键模拟登录状态

export const mockUsers = [
  { id: 1, nickname: "NEO MASTER", avatar: "https://picsum.photos/200/200?random=10" },
  { id: 101, nickname: "设计狂热", avatar: "https://picsum.photos/100/100?random=1" },
  { id: 102, nickname: "穿搭薯", avatar: "https://picsum.photos/100/100?random=2" },
  { id: 103, nickname: "美食家", avatar: "https://picsum.photos/100/100?random=3" },
  { id: 104, nickname: "前端小张", avatar: "https://picsum.photos/100/100?random=4" }
];

export const mockPosts = [
  { 
    id: 1, title: "新野兽主义配色指南", authorName: "设计狂热", authorId: 101, likeCount: 1204, favoriteCount: 450,
    imageUrls: ["https://picsum.photos/400/600?random=101_w400_h600", "https://picsum.photos/400/600?random=105_w400_h600"], 
    hasLiked: false, hasFavorited: false, authorAvatar: "https://picsum.photos/100/100?random=1", sectionId: 1, 
    content: "探索新野兽主义在UI设计中的暴力美学应用。", tags: JSON.stringify(["设计", "UI"])
  },
  { 
    id: 2, title: "深夜机能风审美", authorName: "穿搭薯", authorId: 102, likeCount: 892, favoriteCount: 120,
    imageUrls: ["https://picsum.photos/400/400?random=102_w400_h400"], hasLiked: true, hasFavorited: false,
    authorAvatar: "https://picsum.photos/100/100?random=2", sectionId: 2, 
    content: "机能风不仅仅是口袋多。", tags: JSON.stringify(["穿搭", "机能"])
  },
  { 
    id: 3, title: "成都探店：粗犷咖啡馆", authorName: "美食家", authorId: 103, likeCount: 2501, favoriteCount: 890,
    imageUrls: ["https://picsum.photos/400/700?random=103_w400_h700"], hasLiked: false, hasFavorited: true,
    authorAvatar: "https://picsum.photos/100/100?random=3", sectionId: 1, 
    content: "这家咖啡馆完美诠释了工业风。", tags: JSON.stringify(["美食", "探店"])
  }
];

export const mockSections = [
  { id: 1, displayNameZh: "关注", displayNameEn: "FOLLOW", status: 0 },
  { id: 2, displayNameZh: "发现", displayNameEn: "DISCOVER", status: 0 },
  { id: 3, displayNameZh: "附近", displayNameEn: "NEARBY", status: 0 }
];

export const mockUserInfo = {
  id: 1, nickname: "NEO MASTER", avatar: "https://picsum.photos/200/200?random=10",
  followingCount: 88, followerCount: "12k", totalLikedCount: 51500, totalFavoritedCount: 1500, role: "USER"
};

export const mockOtherUser = {
  id: 102, nickname: "穿搭薯", avatar: "https://picsum.photos/100/100?random=2",
  followingCount: 150, followerCount: "3.2k", totalLikedCount: 8800, isFollowing: false
};

export const mockUnreadCounts = { chat: 2, interaction: 5, system: 0 };

export const mockChatRooms = [
  { id: 1, recipientNickname: "野兽派小秘书", recipientAvatar: "https://picsum.photos/100/100?random=20", lastMessage: "欢迎来到新野兽主义的世界！", lastMessageTime: "2025-12-24T10:00:00", unreadCount: 1 },
  { id: 2, recipientNickname: "设计狂热", recipientAvatar: "https://picsum.photos/100/100?random=1", lastMessage: "你的那篇色彩指南太棒了", lastMessageTime: "2025-12-24T09:30:00", unreadCount: 0 }
];

export const mockChatMessages = [
  { id: 1, senderId: 20, content: "嘿！欢迎加入 SideQuest 社区。" },
  { id: 2, senderId: 1, content: "你好！很高兴能来这里。" },
  { id: 3, senderId: 20, content: "发现你发布的新野兽主义笔记非常受欢迎哦。" },
  { id: 4, senderId: 1, content: "哈哈，我也没想到大家这么喜欢这种硬朗的风格。" },
  { id: 5, senderId: 20, content: "这种风格确实很有视觉冲击力，尤其是在黑夜模式下。" },
  { id: 6, senderId: 1, content: "是啊，我正在尝试让深色模式下的对比度更高一些。" },
  { id: 7, senderId: 20, content: "这是一个好主意。你有参考过其他作品吗？" },
  { id: 8, senderId: 1, content: "看了不少 Pinterest 上的作品，也研究了小红书的排版。" },
  { id: 9, senderId: 20, content: "期待你下一篇更有深度的内容。" },
  { id: 10, senderId: 1, content: "没问题，已经在构思了。" },
  { id: 11, senderId: 20, content: "对了，你对社区的聊天功能有什么建议吗？" },
  { id: 12, senderId: 1, content: "我觉得现在的气泡投影很酷，希望能支持发图。" },
  { id: 13, senderId: 20, content: "收到！后续版本会考虑加入媒体支持。" },
  { id: 14, senderId: 1, content: "太棒了，加油！" },
  { id: 15, senderId: 20, content: "你觉得这个 20 条消息的滚动测试怎么样？" },
  { id: 16, senderId: 1, content: "非常流畅，布局没有任何错位。" },
  { id: 17, senderId: 20, content: "那就好，我们在持续优化细节。" },
  { id: 18, senderId: 1, content: "辛苦了！" },
  { id: 19, senderId: 20, content: "随时联系我。" },
  { id: 20, senderId: 1, content: "OK，下次聊！" }
];

export const mockComments = [
  { id: 1, userId: 101, nickname: "设计狂热", avatar: "https://picsum.photos/100/100?random=1", content: "这篇文章对新野兽主义的分析非常到位！", createTime: "12-24" },
  { id: 2, userId: 102, nickname: "穿搭薯", avatar: "https://picsum.photos/100/100?random=2", content: "配色方案很有参考价值，收藏了。", createTime: "12-24" }
];
