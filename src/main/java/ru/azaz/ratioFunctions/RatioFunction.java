package ru.azaz.ratioFunctions;

import com.googlecode.vkapi.domain.group.VkGroup;

import java.util.concurrent.Future;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Created by AZAZ on 16.03.2016.
 */
public interface RatioFunction extends BiFunction<VkGroup,VkGroup,Future<Double>> {
    //void init();
    default void init(VkGroup group){

    }

}
