package drinkcounter.repository;

import java.time.Instant;

/** When a user last drank. */
public record LastDrink(int drinkerId, Instant timeStamp) {
}
