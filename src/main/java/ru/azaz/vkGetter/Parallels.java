package ru.azaz.vkGetter;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import ru.stachek66.nlp.mystem.holding.MyStem;
import ru.stachek66.nlp.mystem.holding.MyStemApplicationException;
import ru.stachek66.nlp.mystem.holding.Request;
import ru.stachek66.nlp.mystem.model.Info;
import scala.collection.JavaConversions;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by AZAZ on 14.03.2016.
 */
class Parallels implements Runnable {
    Map<String, Double> tmp;
    String text;
    long multiply;
    AtomicLong wordCount;
    GenericObjectPool<MyStem> pool;

    public Parallels(AtomicLong wordCount, Map<String, Double> tmp, String text, long multiply, GenericObjectPool<MyStem> pool) {
        this.pool=pool;
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
