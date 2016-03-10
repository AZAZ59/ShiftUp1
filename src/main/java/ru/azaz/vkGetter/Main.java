package ru.azaz.vkGetter;

import com.googlecode.vkapi.HttpVkApi;
import com.googlecode.vkapi.WallFiler;
import com.googlecode.vkapi.domain.VkOAuthToken;
import com.googlecode.vkapi.domain.group.VkGroup;
import com.googlecode.vkapi.domain.message.VkWallMessage;
import com.googlecode.vkapi.exceptions.VkException;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import ru.stachek66.nlp.mystem.holding.Factory;
import ru.stachek66.nlp.mystem.holding.MyStem;
import ru.stachek66.nlp.mystem.holding.MyStemApplicationException;
import ru.stachek66.nlp.mystem.holding.Request;
import ru.stachek66.nlp.mystem.model.Info;
import scala.Option;
import scala.collection.JavaConversions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class Main {
    static final int num_threads = 0;
    static GenericObjectPool<MyStem> pool = new GenericObjectPool<MyStem>(new BasePooledObjectFactory<MyStem>() {
        @Override
        public MyStem create() throws Exception {
            return new Factory("-gid --format json")
                    .newMyStem("3.0", Option.<File>empty()).get();
            //.newMyStem("3.0", Option.<File>apply(new File("C:\\\\Users\\\\AZAZ\\\\Desktop\\\\DL\\\\stammig\\\\mystem.exe"))).get();
        }

        @Override
        public PooledObject<MyStem> wrap(MyStem myStem) {
            return new DefaultPooledObject<>(myStem);
        }
    });


    public static void main(final String[] args) throws Exception {

        IntStream.range(0, num_threads).forEach(value -> {
            try {
                pool.addObject();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });


        long id = 75601895;
        HttpVkApi vkApi = new HttpVkApi("5303841", "WmX01fbneO4o20eONdF4", "https://oauth.vk.com/blank.html");
        System.out.println(vkApi.getAuthUri());
        VkOAuthToken tok = new VkOAuthToken("00daf086832d7ed7230e88934936d4470aec582bfecf7cf7ec7c5040510fd4386c3a01e3ee3fc19169644", 0, 28499999);

        ShiftUp sf = new ShiftUp();
        Map<VkGroup,Integer> map=sf.numOfIntersectGroup(29627241,vkApi,tok);
        TreeSet<Tuple> set = new TreeSet<>();
        map.forEach((group, integer) -> {
            set.add(new Tuple(group.getGroupName(), integer+.0));
        });

        for(Tuple t:set){
            System.out.println(t.val1+" \t\t\t "+t.val2);
        }


        System.exit(0);
        analizeAllGroupOfPeople(id, vkApi, tok);
        analizeAllGroupOfPeople(59183759, vkApi, tok);

        //29627241
        //30666517
        //20629724 хабр
        //31969346  СТ

        System.exit(0);
        TreeSet<Tuple> set2 = getAndAnalize("ssau_v_teme", vkApi, tok);
        set2.stream().filter(tuple2 -> tuple2.val2 * 100 > 0.5).sorted((t1, t2) -> Double.compare(t1.val2, t2.val2)).forEach(tuple1 -> System.out.println(tuple1));


        TreeSet<Tuple> set1 = getAndAnalize("ssau_v_teme", vkApi, tok);


        HashMap<String, Double> map1 = new HashMap<>();
        set1.forEach(tuple -> map1.put(tuple.val1, tuple.val2));

        HashMap<String, Double> map2 = new HashMap<>();
        set2.forEach(tuple -> map2.put(tuple.val1, tuple.val2));

        TreeSet<Tuple> rs = new TreeSet<>();


        intersection(map1, map2, rs);
        intersection(map2, map1, rs);


        PrintWriter pw2 = new PrintWriter(new File("output_" + "123eed" + ".txt"));


        System.out.println("+===============================");
        rs.forEach(s -> {
            if (s.val2 * 100 >= 0.5) System.out.println(s);
        });
        System.out.println("+===============================");
        System.out.println(rs.parallelStream().collect(Collectors.averagingDouble(tuple -> tuple.val2 * 100)));
        pw2.close();
    }

    private static void analizeAllGroupOfPeople(long id, HttpVkApi vkApi, VkOAuthToken tok) throws VkException {
        Collection<VkGroup> list = vkApi.groupsOfUser(id, tok);
        ExecutorService es = Executors.newFixedThreadPool(20);
        System.out.println(list);
        final int[] i = {0};
        list.stream().forEach(vkGroup -> /*es.submit((Runnable) () -> */{
                    try {
                        System.out.println("Start analizing group " + vkGroup.getGroupName() + " " + (i[0]++) + " from " + list.size());

                        File f = new File("allGroups/" + id + "_" + vkGroup.getGroupName() + ".txt");
                        if (!f.exists()) f.createNewFile();
                        else if (f.length() == 0) {
                            PrintWriter pw = new PrintWriter(f);
                            TreeSet<Tuple> set = getAndAnalize(vkGroup.getGroupId(), vkApi, tok, 20000);
                            set.forEach(tuple -> pw.println(tuple.val1 + " " + (tuple.val2 * 100) + "%"));
                            pw.close();

                        } else {
                            System.err.println("                    " + f + " skipped");
                        }

                        System.out.println("End analizing group " + vkGroup.getGroupName());
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (VkException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
        );//);
        /*es.shutdown();
        try {
            es.awaitTermination(222, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
    }

    private static void intersection(HashMap<String, Double> map1, HashMap<String, Double> map2, TreeSet<Tuple> rs) {
        map1.forEach((s, aDouble) -> {
            if (map2.containsKey(s)) {
                rs.add(new Tuple(s, (map2.get(s) + aDouble) / 2));
                if (aDouble * 100 > 0.5) {
                    System.out.println(s + " " + (aDouble * 100) + " " + (map2.get(s) * 100) + " " + (map2.get(s) + aDouble) * 100 / 2);
                }
            }
        });
    }

    private static TreeSet<Tuple> getAndAnalize(Object grid, HttpVkApi vkApi, VkOAuthToken tok, int limit) throws VkException, FileNotFoundException {
        long start;
        Collection<VkWallMessage> messages = null;
        if (grid instanceof Number) {
            if (grid instanceof Integer) {
                messages = vkApi.lastGroupWallMessages((Integer) grid, WallFiler.ALL, limit, tok);
            }
            if (grid instanceof Long) {
                messages = vkApi.lastGroupWallMessages((Long) grid, WallFiler.ALL, limit, tok);
            }
        }
        if (grid instanceof String) {
            messages = vkApi.lastGroupWallMessages((String) grid, WallFiler.ALL, limit, tok);
        }

        Map<String, Double> tmp = Collections.synchronizedMap(new HashMap<>());

        /*PrintWriter pw = new PrintWriter(new File("input_test12.txt"));
        messages.forEach(vkWallMessage ->
                pw.println(
                        "======" + vkWallMessage.getText() + "==" + vkWallMessage.getCommentsCount() + "==" + vkWallMessage.getLikesCount() + "==" + vkWallMessage.getRepostCount() + "======"
                )
        );
        pw.close();*/


        start = System.currentTimeMillis();


        final StringBuilder[] sb = {new StringBuilder("")};
        ExecutorService es = Executors.newFixedThreadPool(num_threads);
        AtomicLong wordCount = new AtomicLong(0);
        final int[] i = {0};
        messages.forEach(vkWallMessage -> {
            i[0]++;
            sb[0].append(vkWallMessage.getText());
            if (i[0] % 200 == 0) {
                es.execute(new Parallels(wordCount, tmp, sb[0].toString(), vkWallMessage.getCommentsCount() + vkWallMessage.getLikesCount() + vkWallMessage.getRepostCount() + 1));
                sb[0] = new StringBuilder("");
            }
        });
        es.shutdown();
        try {
            System.err.println(es.awaitTermination(48, TimeUnit.HOURS));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        TreeSet<Tuple> set = new TreeSet<>();
        tmp.entrySet().stream().forEach(
                stringDoubleEntry -> set.add(
                        new Tuple(stringDoubleEntry.getKey(), stringDoubleEntry.getValue() / wordCount.get())
                )
        );
        return set;
    }

    private static TreeSet<Tuple> getAndAnalize(Object grid, HttpVkApi vkApi, VkOAuthToken tok) throws VkException, FileNotFoundException {
        return getAndAnalize(grid, vkApi, tok, 0);
    }


}

class Parallels implements Runnable {
    Map<String, Double> tmp;
    String sss;
    long multiply;
    AtomicLong wordCount;

    public Parallels(AtomicLong wordCount, Map<String, Double> tmp, String sss, long multiply) {
        this.tmp = tmp;
        this.sss = sss;
        this.multiply = multiply;
        this.wordCount = wordCount;
    }

    @Override
    public void run() {
        analizeAndMultiplyString(wordCount, tmp, sss, multiply);
    }

    void analizeAndMultiplyString(AtomicLong wordCount, Map<String, Double> tmp, String sss, long multiply) {
        MyStem mystemAnalyzer = null;
        try {
            mystemAnalyzer = Main.pool.borrowObject();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Iterable<Info> result = null;
        try {
            result = JavaConversions.asJavaIterable(
                    mystemAnalyzer
                            .analyze(Request.apply(sss))
                            .info()
                            .toIterable());
        } catch (MyStemApplicationException e) {
            e.printStackTrace();
        }
        ObjectMapper map = new ObjectMapper();

        //HashMap<String, Double> tmp = new HashMap<>();

        //long LongWordCount = 0;

        for (final Info info : result) {

            JsonNode node = null;
            try {
                node = map.readTree(info.rawResponse()).get("analysis").get(0);
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                String key = node.get("lex").getTextValue();
                String type = node.get("gr").getTextValue() + ",";
                type = type.substring(0, type.indexOf(',')).replaceAll("=", "");
                if (key.equals("е")) {
                    //System.out.println("");
                }
                if (/*type.equalsIgnoreCase("V") ||*/ type.equalsIgnoreCase("A") || type.equalsIgnoreCase("S")) {
                    if (!tmp.containsKey(key)) {
                        tmp.put(key, 0d);
                    }
                    tmp.put(key, tmp.get(key) + multiply);
                    wordCount.addAndGet(multiply);
                }
            } catch (Exception e) {
            }
        }
        Main.pool.returnObject(mystemAnalyzer);
    }
}
