package ru.azaz.vkGetter;

import com.googlecode.vkapi.domain.group.VkGroup;
import ru.azaz.groupsGetter.GetGroupFunction;
import ru.azaz.ratioFunctions.RatioFunction;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Created by AZAZ on 16.03.2016.
 */
public class GroupRatioSetter {
    Map<VkGroup,Double> groupsRatio(VkGroup mainGroup, Collection<VkGroup> groups, RatioFunction... functions){
        Map<VkGroup,Double> result = new HashMap<>();
        for(VkGroup currentGroup:groups){
            result.put(
                    currentGroup,
                    Arrays.stream(functions).parallel().peek(ratioFunction -> ratioFunction.init(mainGroup))
                            .collect(
                                    //Collectors.averagingDouble(
                                    Collectors.summingDouble(
                                            value -> {
                                                try {
                                                    return value.apply(mainGroup, currentGroup).get();
                                                } catch (InterruptedException e) {
                                                    e.printStackTrace();
                                                } catch (ExecutionException e) {
                                                    e.printStackTrace();
                                                }
                                                return 0;
                                            }
                                    )
                            )
            );
        }
        return result;
    }
    Map<VkGroup,Double> groupsRatio(VkGroup mainGroup, GetGroupFunction getter, RatioFunction... functions){
        return groupsRatio(mainGroup, getter.apply(mainGroup), functions);
    }
}
