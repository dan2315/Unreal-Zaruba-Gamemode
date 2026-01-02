package com.dod.unrealzaruba.Gamemodes.GamemodeData;

import java.io.IOException;

import com.dod.unrealzaruba.Gamemodes.BaseGamemode;


public interface GamemodeData<T> {
    Class<T> getDataClass();

    Class<? extends BaseGamemode> getGamemodeClass();

    String getDataName();

    T getData();

    void setData(T data);

    T loadData();

    void saveData() throws IOException;
} 