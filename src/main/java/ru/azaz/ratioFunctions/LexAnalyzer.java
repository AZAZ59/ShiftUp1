package ru.azaz.ratioFunctions;

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
import ru.azaz.VkFile;
import ru.azaz.utils.Intersection;
import ru.azaz.vkGetter.Tuple;
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
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Created by AZAZ on 14.03.2016.
 */
public class LexAnalyzer extends VkFile implements RatioFunction {
    int numDownloadThreads = 20;
    int numParseThread = 4;
    GenericObjectPool<MyStem> pool = new GenericObjectPool<MyStem>(new BasePooledObjectFactory<MyStem>() {
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

    static Future<TreeSet<Tuple<String, Double>>> set;


    public LexAnalyzer(HttpVkApi vkApi, VkOAuthToken tok, int numDownloadThreads) {
        this(vkApi, tok, numDownloadThreads, 4);
    }

    public LexAnalyzer(HttpVkApi vkApi, VkOAuthToken tok, int numDownloadThreads, int numParseThread) {
        this.vkApi = vkApi;
        this.tok = tok;
        this.numDownloadThreads = numDownloadThreads;
        this.numParseThread = numParseThread;

        IntStream.range(0, numParseThread).forEach(value -> {
            try {
                pool.addObject();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    Future<TreeSet<Tuple<String, Double>>> asyncGetAndAnalize(Object grid, int limit) {
        FutureTask task = new FutureTask<>(() -> {
            System.out.println("Start " + grid);
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

            final StringBuilder[] sb = {new StringBuilder("")};
            ExecutorService es = Executors.newFixedThreadPool(numDownloadThreads);
            AtomicLong wordCount = new AtomicLong(0);
            final int[] i = {0};
            messages.forEach(vkWallMessage -> {
                i[0]++;
                sb[0].append(vkWallMessage.getText());
                if (i[0] % 200 == 0) {
                    es.execute(new Parallels(wordCount, tmp, sb[0].toString(), vkWallMessage.getCommentsCount() + vkWallMessage.getLikesCount() + vkWallMessage.getRepostCount() + 1, pool));
                    sb[0] = new StringBuilder("");
                }
            });
            es.shutdown();
            try {
                System.err.println(es.awaitTermination(48, TimeUnit.HOURS));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            TreeSet<Tuple<String, Double>> set = new TreeSet<>();
            tmp.entrySet().stream().forEach(
                    stringDoubleEntry -> set.add(
                            new Tuple<>(stringDoubleEntry.getKey(), stringDoubleEntry.getValue() / wordCount.get())
                    )
            );
            VkGroup gr = vkApi.groupInfo(grid.toString(), tok);
            set.add(new Tuple<>("*-*-*-*-*-*-*-*-*-*-*-*-", gr.getGroupId() + .0));
            System.out.println("End " + grid);
            return set;
        });
        task.run();
        return task;
    }

    Future<TreeSet<Tuple<String, Double>>> asyncGetAndAnalize(Object grid) {
        return asyncGetAndAnalize(grid, 0);
    }

    TreeSet<Tuple<String, Double>> getAndAnalize(Object grid, int limit) throws VkException, FileNotFoundException {
        try {
            return asyncGetAndAnalize(grid, limit).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    TreeSet<Tuple<String, Double>> getAndAnalize(Object grid) throws VkException, FileNotFoundException {
        return getAndAnalize(grid, 0);
    }

    @Override
    public Future<Double> apply(VkGroup group, VkGroup group2) {
        Future<TreeSet<Tuple<String, Double>>> set2 = asyncGetAndAnalize(group2);
        FutureTask<Double> task = new FutureTask<Double>(
                () -> (double) Intersection.intersect(set.get(), set2.get()).size()
        );
        task.run();
        return task;
    }

    @Override
    public void init(VkGroup group) {
        set = asyncGetAndAnalize(group.getGroupId());
    }
}

class Parallels implements Runnable {
    Map<String, Double> tmp;
    String text;
    long multiply;
    AtomicLong wordCount;
    GenericObjectPool<MyStem> pool;

    public Parallels(AtomicLong wordCount, Map<String, Double> tmp, String text, long multiply, GenericObjectPool<MyStem> pool) {
        this.pool = pool;
        this.tmp = tmp;
        this.text = text;
        this.multiply = multiply;
        this.wordCount = wordCount;
    }

    @Override
    public void run() {
        analizeAndMultiplyString(wordCount, tmp, text, multiply);
    }

    void analizeAndMultiplyString(AtomicLong wordCount, Map<String, Double> tmp, String sss, long multiply) {
        MyStem mystemAnalyzer = null;
        try {
            mystemAnalyzer = pool.borrowObject();
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
                if (type.equalsIgnoreCase("A") || type.equalsIgnoreCase("S")) {
                    if (!tmp.containsKey(key)) {
                        tmp.put(key, 0d);
                    }
                    tmp.put(key, tmp.get(key) + multiply);
                    wordCount.addAndGet(multiply);
                }
            } catch (Exception e) {
            }
        }
        pool.returnObject(mystemAnalyzer);
    }
}
