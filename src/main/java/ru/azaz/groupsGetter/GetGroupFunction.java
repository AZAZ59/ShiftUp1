package ru.azaz.groupsGetter;

import com.googlecode.vkapi.domain.group.VkGroup;

import java.util.Collection;
import java.util.concurrent.Future;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Created by AZAZ on 16.03.2016.
 */
public interface GetGroupFunction extends Function<VkGroup,Collection<VkGroup>> {}
