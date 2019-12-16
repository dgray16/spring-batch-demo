package com.example.demo2.domain.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@ToString
@Table("items")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Item {

    @Id
    Long id;

    Integer originalValue;
    Integer modifiedValue;

}
