package com.pigeostudios.pwp.warfare.client;

import java.util.Map;
import net.minecraft.nbt.CompoundTag;

// Буфер обмена для копирования/вставки наборов экипировки
// Хранит данные набора и карту наборов для всей команды
public class WarfareClipboard {
   public static CompoundTag kitData = null;
   public static Map<String, CompoundTag> teamKitsData = null;
}
