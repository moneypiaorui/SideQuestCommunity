package com.sidequest.search.infrastructure;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@Document(indexName = "posts")
public class PostDoc {
    @Id
    private String id;
    
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String title;
    
    @Field(type = FieldType.Text, analyzer = "ik_smart")
    private String content;
    
    @Field(type = FieldType.Keyword)
    private String authorName;
    
    @Field(type = FieldType.Long)
    private Long sectionId;
    
    @Field(type = FieldType.Integer)
    private Integer status;
    
    @Field(type = FieldType.Integer)
    private Integer likeCount;
    
    @Field(type = FieldType.Integer)
    private Integer commentCount;
    
    @Field(type = FieldType.Integer)
    private Integer favoriteCount;
    
    @Field(type = FieldType.Integer)
    private Integer viewCount;
    
    @Field(type = FieldType.Long)
    private Long createTime;
}

