package mm4j.interfaces;

import mm4j.annotation.Constructor;
import mm4j.annotation.Mapping;
import mm4j.records.*;

public interface RecordMapper {
    String stringToString(final String string);

    ToRecordOne multiParamsMap(final FromRecordOne fromRecordOne);

    ToRecordTwo singleParamMap(final FromRecordOne fromRecordOne);

    ToRecordThree multiDefaultConstructorMap(final FromRecordOne fromRecordOne);

    ToRecordThree multiConstructorMap(final FromRecordTwo fromRecordTwo);

    @Constructor(mappings = {"id", "surName"})
    ToRecordThree multiAnnotatedConstructorMap(final FromRecordThree fromRecordThree);

    @Constructor(mappings = {"id", "surname"}, caseSensitive = false)
    ToRecordThree multiAnnotatedConstructorMapCaseInsensitive(final FromRecordThree fromRecordThree);

    @Mapping(mapFrom = "surName", mapTo = "firstName")
    @Mapping(mapFrom = "firstname", mapTo = "surname", caseSensitive = false)
    ToRecordOne swapFirstAndLastName(final FromRecordOne fromRecordOne);
}
