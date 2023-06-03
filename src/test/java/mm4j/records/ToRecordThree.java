package mm4j.records;

import java.util.UUID;

public record ToRecordThree(UUID id, String firstName, String surName) {

    public ToRecordThree(String firstName, String surName) {
        this(UUID.randomUUID(), firstName, surName);
    }

    public ToRecordThree(UUID id, String surName) {
        this(id, "Eva", surName);
    }
}
