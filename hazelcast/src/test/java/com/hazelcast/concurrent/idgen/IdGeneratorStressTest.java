package com.hazelcast.concurrent.idgen;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IdGenerator;
import com.hazelcast.test.HazelcastTestRunner;
import com.hazelcast.test.HazelcastTestSupport;
import com.hazelcast.test.TestHazelcastInstanceFactory;
import com.hazelcast.test.annotation.QuickTest;
import com.hazelcast.test.annotation.RunParallel;
import com.hazelcast.util.RandomPicker;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunParallel
@RunWith(HazelcastTestRunner.class)
@Category({QuickTest.class})
public class IdGeneratorStressTest extends HazelcastTestSupport {

    private static final int THREAD_COUNT = 16;

    private static final int NUMBER_OF_IDS_PER_THREAD = 300001;

    private static final int TOTAL_ID_GENERATED = THREAD_COUNT * NUMBER_OF_IDS_PER_THREAD;

    @Parameterized.Parameter(0)
    public int clusterSize;
    String name;
    HazelcastInstance[] instances;

    @Before
    public void setup() {
        TestHazelcastInstanceFactory factory = createHazelcastInstanceFactory(clusterSize);
        instances = factory.newInstances();
        name = randomString();
    }

    @Test
    public void testMultipleThreads() throws ExecutionException, InterruptedException {
        pickIdGenerator().init(13013);

        List<Future> futureList = new ArrayList<Future>(THREAD_COUNT);
        for (int i = 0; i < THREAD_COUNT; i++) {
            IdGenerator idGenerator = pickIdGenerator();
            IdGeneratorCallable callable = new IdGeneratorCallable(idGenerator);
            Future<long[]> future = spawn(callable);
            futureList.add(future);
        }

        Set totalGeneratedIds = new HashSet<Long>(TOTAL_ID_GENERATED);
        for (Future<long[]> future : futureList) {
            long[] generatedIds = future.get();
            for (long generatedId : generatedIds) {
                assertTrue("ID: " + generatedId, totalGeneratedIds.add(generatedId));
            }
        }

        assertEquals(TOTAL_ID_GENERATED, totalGeneratedIds.size());
    }

    private IdGenerator pickIdGenerator() {
        int index = RandomPicker.getInt(instances.length);
        return instances[index].getIdGenerator(name);
    }

    @Parameterized.Parameters(name = "clusterSize:{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {1},
                {6},
        });
    }

    private static class IdGeneratorCallable implements Callable<long[]> {

        IdGenerator idGenerator;

        public IdGeneratorCallable(IdGenerator idGenerator) {
            this.idGenerator = idGenerator;
        }

        @Override
        public long[] call() throws Exception {
            long[] generatedIds = new long[NUMBER_OF_IDS_PER_THREAD];
            for (int j = 0; j < NUMBER_OF_IDS_PER_THREAD; j++) {
                generatedIds[j] = idGenerator.newId();
            }
            return generatedIds;
        }
    }

}
