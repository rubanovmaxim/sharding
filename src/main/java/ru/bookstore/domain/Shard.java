package ru.bookstore.domain;

import java.util.List;

public class Shard {

    private String url;

    public Shard(String url) {
        this.url = url;
    }

    public void insert(Book book) {
        //................  заводим новую книгу
    }

    public List<Book> find(String searchString) {
        List<Book> result = null;
        //............
        return result;
    }

}
