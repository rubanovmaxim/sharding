package ru.bookstore.sharding.rest;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.bookstore.config.YamlConfig;
import ru.bookstore.domain.Book;
import ru.bookstore.domain.Shard;

import javax.annotation.PostConstruct;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
public class ShardingController {


    private int shardCount;
    private List<Shard> shards = new ArrayList<>();

    private YamlConfig config;

    public ShardingController(YamlConfig config) {
        this.config = config;
    }


    // инициализируем shard-ы
    @PostConstruct
    private void init() {
        config.getShards().forEach(url -> {
            shards.add(new Shard(url));
        });
        shardCount = shards.size();
    }

    @PostMapping("/sharding/books/new")
    public ResponseEntity addBook(@RequestBody(required = true) Book book) {
        String sql = formSqlShardCriteria(book.getName());
        int hash = sha256(sql);
        int shardNum = hash % shardCount;
        shards.get(shardNum).insert(book);

        return ResponseEntity.ok().body(book);
    }

    @GetMapping("/sharding/book/find/{requestString}")
    public ResponseEntity<List<Book>> findBooks(@PathVariable("requestString") String requestString) {
        List<Book> result = new ArrayList<>();

        if (requestString == null || requestString.isEmpty()) {
            ResponseEntity.ok().body(result);
        }
        String[] worlds = requestString.trim().split("\\s+");
        //если в поисковом запросе только одно слово, тоискать надо везде
        if (worlds.length == 1) {
            result = fullSearch(requestString, null);
            return ResponseEntity.ok().body(result);
        }

        String sql = formSqlShardCriteria(requestString);
        int hash = sha256(sql);
        int shardNum = hash % shardCount;

        result = shards.get(shardNum).find(requestString);

        // проверяем , нашли ли книги на нужном shard, если ихз нет - нудно искать на оставшихся,
        // т.к. критерии могут быть не полными.
        if (result.size() == 0) {
            result = fullSearch(requestString, shards.get(shardNum));
        }

        return ResponseEntity.ok().body(result);
    }


    private List<Book> fullSearch(String searchString, Shard exceptShard) {
        List<Book> result = new ArrayList<>();
        // ......................
        //TODO  здесь формируем строку поиска ну или объект поиска, смотря как будем реализовывать.
        //......................

        for (Shard shard : shards) {
            // исключаем из поиска shard, если передали не пустое значение его.
            // Это нужно например если мы не нащшли по запросу в одном из них и поняли что надо искать по оставшимся.
            // либо какой то отвалился и мы об этом узнали.
            if (shard == exceptShard) {
                continue;
            }
            // резыльтаты поиска по каждому shard заносим в общий результат.
            result.addAll(shard.find(searchString));
        }
        return result;
    }

    // функция формируетс строку для которй потом берем hash
    //Строка формируется как часть запроса SQL для поиска книги, например - like '%выно%напра%' or like '%напра%выно%'
    private static String formSqlShardCriteria(String name) {
        String result = "like '%";
        String or = "";
        String[] worlds = name.trim().split("\\s+");
        // если всего одно слово в названии - просто возвращаем его
        if (worlds.length == 1) {
            return name;
        }
        for (int i = 0; i < worlds.length && i < 2; i++) {
            ///не учитываем в поиске предлоги, пропускаем такие слова, длинна колорых меньше 3-х букв1
            if (worlds[i].length() < 3) {
                continue;
            }
            //4 буквы или 3, если слово из 3-x букв.
            result = result + new String(Arrays.copyOf(worlds[i].toCharArray(), 4)).trim() + '%';
            or = new String(Arrays.copyOf(worlds[i].toCharArray(), 4)).trim() + '%' + or;
        }
        or = " or like '%" + or + "'";
        return result + "' " + or;
    }

    //получаем хеш
    private static int sha256(String data) {
        MessageDigest md;
        StringBuilder sb = new StringBuilder();
        try {
            md = MessageDigest.getInstance("SHA-256");
            byte[] hashInBytes = md.digest(data.getBytes(StandardCharsets.UTF_8));
            // bytes to hex

            for (byte b : hashInBytes) {
                sb.append(String.format("%02x", b));
            }
            BigInteger num = new BigInteger(sb.toString(), 16);
            sb.setLength(0);
            return num.intValue();

        } catch (NoSuchAlgorithmException e) {

            e.printStackTrace();
            return -1;
        }
    }

}
