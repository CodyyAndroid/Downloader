package com.codyy.downloader;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
        Map<String,String> map=new HashMap<>();
        map.put("1","111");
        map.put("2","2222");
        map.remove("1");
        assertEquals(1,map.size());
    }
}