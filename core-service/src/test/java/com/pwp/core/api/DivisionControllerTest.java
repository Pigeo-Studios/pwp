package com.pwp.core.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pwp.core.db.DivisionRepository;
import io.javalin.http.BadRequestResponse;
import org.junit.jupiter.api.Test;

class DivisionControllerTest {

    @Test
    void normalizesValidBody() {
        DivisionRepository.Row r = DivisionController.parse(
                "USA",
                "mech",
                JsonParser.parseString("{\"modes\":\"aas, invasion\",\"mapSizes\":\"large,medium\","
                        + "\"vehicles\":[{\"role\":\"apc\",\"vehicleName\":\"usa_stryker\",\"count\":50}]}"));
        assertEquals("usa", r.faction);
        assertEquals("mech", r.displayName);
        assertEquals("AAS,INVASION", r.modes);
        assertEquals("LARGE,MEDIUM", r.mapSizes);
        assertEquals(
                "[{\"role\":\"APC\",\"vehicleName\":\"usa_stryker\",\"count\":20,\"respawnSeconds\":0}]", r.vehicles);
        r.updatedAt = 5;
        JsonObject j = DivisionController.toJson(r);
        assertEquals("mech", j.get("divisionId").getAsString());
        assertEquals(1, j.getAsJsonArray("vehicles").size());
    }

    @Test
    void rejectsBadInput() {
        assertThrows(BadRequestResponse.class, () -> DivisionController.parse("usa", "x", JsonParser.parseString("[]")));
        assertThrows(
                BadRequestResponse.class,
                () -> DivisionController.parse("usa", "x", JsonParser.parseString("{\"vehicles\":5}")));
        assertThrows(
                BadRequestResponse.class,
                () -> DivisionController.parse(
                        "usa", "x", JsonParser.parseString("{\"mapSizes\":\"HUGE\",\"vehicles\":[]}")));
        assertThrows(
                BadRequestResponse.class,
                () -> DivisionController.parse(
                        "usa", "x", JsonParser.parseString("{\"vehicles\":[{\"role\":\"A PC\",\"vehicleName\":\"v\"}]}")));
        assertThrows(
                BadRequestResponse.class,
                () -> DivisionController.parse("us a", "x", JsonParser.parseString("{\"vehicles\":[]}")));
    }
}
