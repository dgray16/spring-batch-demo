package com.example.demo2.domain.service;

import com.example.demo2.domain.model.Item;
import com.example.demo2.domain.repository.ItemRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.shaded.com.google.common.collect.Lists;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ItemService {

    ItemRepository itemRepository;

    @Transactional(readOnly = true)
    public List<Item> findAll() {
        return Lists.newArrayList(itemRepository.findAll());
    }

}
