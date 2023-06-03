package mm4j;

import mm4j.interfaces.RecordMapper;
import mm4j.records.FromRecordOne;
import mm4j.records.FromRecordThree;
import mm4j.records.FromRecordTwo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.UUID;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MM4JTest {

    private RecordMapper recordMapper;

    @BeforeAll
    private void setup() {
        recordMapper = MM4J.createMapper(RecordMapper.class);
    }

    @Test
    public void stringToStringTest() {
        final var result = recordMapper.stringToString("jojo");

        Assertions.assertEquals("jojo", result);
    }

    @Test
    public void multiParamsMapTest1() {
        final var input = new FromRecordOne(UUID.randomUUID(), "Justin", "Henderson");
        final var result = recordMapper.multiParamsMap(input);

        Assertions.assertNotNull(result);
        Assertions.assertEquals("Justin", result.firstName());
        Assertions.assertEquals("Henderson", result.surName());
    }

    @Test
    public void multiParamsMapTest2() {
        final var input = new FromRecordOne(UUID.randomUUID(), "Patrick", "Henderson");
        final var result = recordMapper.multiParamsMap(input);

        Assertions.assertNotNull(result);
        Assertions.assertEquals("Patrick", result.firstName());
        Assertions.assertEquals("Henderson", result.surName());
    }

    @Test
    public void singleParamMapTest() {
        final var input = new FromRecordOne(UUID.randomUUID(), "Patrick", "Henderson");
        final var result = recordMapper.singleParamMap(input);

        Assertions.assertNotNull(result);
        Assertions.assertEquals("Henderson", result.surName());
    }

    @Test
    public void multiConstructorMapTest() {
        final var input = new FromRecordTwo("Patrick", "Henderson");
        final var result = recordMapper.multiConstructorMap(input);

        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.id());
        Assertions.assertEquals("Patrick", result.firstName());
        Assertions.assertEquals("Henderson", result.surName());
    }

    @Test
    public void multiAnnotatedConstructorMapTest() {
        final var input = new FromRecordThree(UUID.randomUUID(), "Netherlands", "Henderson");
        final var result = recordMapper.multiAnnotatedConstructorMap(input);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(input.id(), result.id());
        Assertions.assertEquals("Eva", result.firstName());
        Assertions.assertEquals("Henderson", result.surName());
    }

    @Test
    public void multiDefaultConstructorMapTest() {
        final var input = new FromRecordOne(UUID.randomUUID(), "Patrick", "Henderson");
        final var result = recordMapper.multiDefaultConstructorMap(input);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(input.id(), result.id());
        Assertions.assertEquals("Patrick", result.firstName());
        Assertions.assertEquals("Henderson", result.surName());
    }

    @Test
    public void multiAnnotatedConstructorMapCaseInsensitive() {
        final var input = new FromRecordThree(UUID.randomUUID(), "Netherlands", "Henderson");
        final var result = recordMapper.multiAnnotatedConstructorMapCaseInsensitive(input);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(input.id(), result.id());
        Assertions.assertEquals("Eva", result.firstName());
        Assertions.assertEquals("Henderson", result.surName());
    }

}
