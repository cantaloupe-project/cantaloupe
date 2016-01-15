package edu.illinois.library.cantaloupe.script;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class RubyScriptEngineTest {

    RubyScriptEngine instance;

    @Before
    public void setUp() {
        instance = new RubyScriptEngine();
    }

    @Test
    public void testInvoke() throws Exception {
        final String code = "module Cantaloupe\n" +
                "SOMETHING = 1\n" +
                "def self.func(arg)\n" +
                "arg\n" +
                "end\n" +
                "end";
        instance.load(code);
        String function = "Cantaloupe::func";

        for (int i = 0; i < 3; i++) {
            String[] args = { String.valueOf(i) };
            String result = (String) instance.invoke(function, args);
            assertEquals(String.valueOf(i), result);
        }
    }

    @Test
    public void testInvokeWithUnexpectedReturnType() throws Exception {
        final String code = "module Cantaloupe\n" +
                "def self.func\n" +
                "[]\n" +
                "end\n" +
                "end";
        instance.load(code);
        String function = "Cantaloupe::func";

        try {
            Map result = (Map) instance.invoke(function);
            fail("Shouldn't be able to cast an array to a map");
        } catch (ClassCastException e) {
            // pass
        }
    }

    @Test
    public void testMethodExists() throws Exception {
        final String code = "module Cantaloupe\n" +
                "def self.func(arg)\n" +
                "end\n" +
                "def self.func2\n" +
                "end\n" +
                "end";
        instance.load(code);
        assertTrue(instance.methodExists("func"));
        assertFalse(instance.methodExists("bogus"));
    }

    @Test
    public void testSerializeAsRuby() throws Exception {
        final List<Object> collection1 = new ArrayList<>();
        collection1.add(123);
        final List<Object> collection2 = new ArrayList<>();
        collection2.add(645);
        collection2.add(collection1);
        collection2.add("dog's");

        // LinkedHashMap preserves order for testability
        final Map<Object,Object> map1 = new LinkedHashMap<>();
        map1.put("key1", 123);
        final Map<Object,Object> map2 = new LinkedHashMap<>();
        map2.put("key1", 645);
        map2.put(152, map1);
        map2.put("cat's", "dog's");
        map2.put(new File("/dev/null"), "bla"); // object as key

        final List<Object> args = new ArrayList<>();
        args.add(null); // null
        args.add("it's"); // string
        args.add(5362); // int
        args.add(53.62f); // float
        args.add(true); // boolean
        args.add(false); // boolean
        args.add(new ArrayList<>()); // empty collection
        args.add(collection2); // collection
        args.add(new HashMap<>()); // empty map
        args.add(map2); // map
        args.add(new File("/dev/null")); // Object
        assertEquals("[nil, 'it\\'s', 5362, 53.62, true, false, " +
                        "[], [645, [123], 'dog\\'s'], " +
                        "{}, {'key1' => 645, '152' => {'key1' => 123}, 'cat\\'s' => 'dog\\'s', '/dev/null' => 'bla'}, " +
                        "'/dev/null']",
                instance.serializeAsRuby(args));
    }

}
