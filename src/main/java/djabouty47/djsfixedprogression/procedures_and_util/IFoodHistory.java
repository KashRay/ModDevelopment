package djabouty47.djsfixedprogression.procedures_and_util;

import java.util.Deque;

public interface IFoodHistory {
    Deque<String> djs$getFoodHistory();
    void djs$addFoodToHistory(String itemId);
    float djs$getCurrentMultiplier(String itemId);
}
