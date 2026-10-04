package com.dod.unrealzaruba.utils.Geometry;

import net.minecraft.core.Direction;
import org.joml.Quaterniond;


public class Utils {
    public static Quaterniond getQuatFromDir(Direction direction) {
        Quaterniond quaternion = new Quaterniond();

        switch (direction) {
            case UP:
                quaternion.rotationY(Math.PI); // was identity
                break;
            case DOWN:
                quaternion.identity(); // was π
                break;
            case NORTH:
                quaternion.rotationY((Math.PI / 2) * 2 + Math.PI); // was π
                break;
            case SOUTH:
                quaternion.rotationY(Math.PI); // was 0
                break;
            case EAST:
                quaternion.rotationY(Math.PI / 2 + Math.PI); // was π/2
                break;
            case WEST:
                quaternion.rotationY(-Math.PI / 2 + Math.PI); // was -π/2
                break;
            default:
                quaternion.rotationY(Math.PI); // was identity
        }

        return quaternion;
    }

    public static float getYRotForDirection(Direction direction) {
        float yRotation = 0;
        switch (direction) {
            case NORTH: yRotation = 180; break;
            case SOUTH: yRotation = 0; break;
            case WEST: yRotation = 90; break;
            case EAST: yRotation = 270; break;
            default: yRotation = 0; break;
        }
        return yRotation;
    }
}
