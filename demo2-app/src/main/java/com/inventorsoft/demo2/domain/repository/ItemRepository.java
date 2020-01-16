package com.inventorsoft.demo2.domain.repository;

import com.inventorsoft.demo2.domain.model.Item;
import org.springframework.data.repository.CrudRepository;

public interface ItemRepository extends CrudRepository<Item, Long> {
}
