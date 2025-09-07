package com.dod.UnrealZaruba.Utils.Geometry;

import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.core.impl.game.ships.ShipTransformImpl;
import org.joml.Quaterniondc;
import org.joml.Quaterniond;
import org.joml.Vector3d;


public class Utils {
    public static ShipTransform RotateAroundCenter(ShipTransform rotationPoint, ShipTransform transform, Quaterniondc rotation) {
        Vector3d shipPosition = new Vector3d(transform.getPositionInWorld());
        Quaterniondc shipRotation = transform.getShipToWorldRotation();

        Vector3d shipYardCenter = transform.getWorldToShip().transformPosition(shipPosition);

        ShipTransform rotatedCenter = ShipTransformImpl.Companion.create(
            rotationPoint.getPositionInWorld(),
            rotationPoint.getPositionInShip(),
            rotationPoint.getShipToWorldRotation().mul(rotation, new Quaterniond()),
            rotationPoint.getShipToWorldScaling()
        );

        Quaterniond difference = rotatedCenter.getShipToWorldRotation()
        .mul(rotationPoint.getShipToWorldRotation()
        .invert(new Quaterniond()), new Quaterniond());
        
        Quaterniond newRotation = difference.mul(shipRotation, new Quaterniond());
        Vector3d newPosition = rotatedCenter.getShipToWorld().transformPosition(shipYardCenter);

        return ShipTransformImpl.Companion.create(
            newPosition,
            transform.getPositionInShip(),
            newRotation,
            transform.getShipToWorldScaling()
        );
    }

    public static Quaterniond getQuatFromDir(Direction direction) {
        Quaterniond quaternion = new Quaterniond();
        
        switch (direction) {
            case UP:
                quaternion.identity();
                break;
            case DOWN:
                quaternion.rotationY(Math.PI);
                break;
            case NORTH:
                quaternion.rotationY((Math.PI / 2) * 2);
                break;
            case SOUTH:
                quaternion.rotationY((Math.PI / 2) * 0);
                break;
            case EAST:
                quaternion.rotationY(Math.PI / 2);
                break;
            case WEST:
                quaternion.rotationY(-Math.PI / 2);
                break;
            default:
                quaternion.identity();
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
