package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FieldDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;

public class FieldDefinitionCoursePoint extends FieldDefinition {

    public FieldDefinitionCoursePoint(int localNumber, int size, BaseType baseType, String name) {
        super(localNumber, size, baseType, name, 1, 0);
    }

    @Override
    public Object decode(ByteBuffer byteBuffer) {
        final Object rawObj = baseType.decode(byteBuffer, scale, offset);
        if (rawObj != null) {
            final int raw = (int) rawObj;
            return CoursePoint.fromId(raw);
        }
        return null;
    }

    @Override
    public void encode(ByteBuffer byteBuffer, Object o) {
        if (o instanceof CoursePoint) {
            baseType.encode(byteBuffer, (((CoursePoint) o).getId()), scale, offset);
            return;
        }
        baseType.encode(byteBuffer, o, scale, offset);
    }

    //https://github.com/mshroyer/coursepointer/blob/d0eb541c1368f6bfdde8406a3a6cb91f6dacc4d2/src/fit.rs#L621
    public enum CoursePoint {
        GENERIC(0, "Generic"),
        SUMMIT(1, "Summit"),
        VALLEY(2, "Valley"),
        WATER(3, "Water"),
        FOOD(4, "Food"),
        DANGER(5, "Danger"),
        LEFT(6, "Left"),
        RIGHT(7, "Right"),
        STRAIGHT(8, "Straight"),
        FIRSTAID(9, "FirstAid"),
        FOURTHCATEGORY(10, "FourthCategory"),
        THIRDCATEGORY(11, "ThirdCategory"),
        SECONDCATEGORY(12, "SecondCategory"),
        FIRSTCATEGORY(13, "FirstCategory"),
        HORSCATEGORY(14, "HorsCategory"),
        SPRINT(15, "Sprint"),
        LEFTFORK(16, "LeftFork"),
        RIGHTFORK(17, "RightFork"),
        MIDDLEFORK(18, "MiddleFork"),
        SLIGHTLEFT(19, "Left_slight"),
        SHARPLEFT(20, "Left_sharp"),
        SLIGHTRIGHT(21, "Right_slight"),
        SHARPRIGHT(22, "Right_sharp"),
        UTURN(23, "UTurn"),
        SEGMENTSTART(24, "SegmentStart"),
        SEGMENTEND(25, "SegmentEnd"),
        CAMPSITE(27, "Campsite"),
        AIDSTATION(28, "AidStation"),
        RESTAREA(29, "RestArea"),
        GENERALDISTANCE(30, "GeneralDistance"),    // Used with UpAhead
        SERVICE(31, "Service"),
        ENERGYGEL(32, "EnergyGel"),
        SPORTSDRINK(33, "SportsDrink"),
        MILEMARKER(34, "MileMarker"),
        CHECKPOINT(35, "Checkpoint"),
        SHELTER(36, "Shelter"),
        MEETINGSPOT(37, "MeetingSpot"),
        OVERLOOK(38, "Overlook"),
        TOILET(39, "Toilet"),
        SHOWER(40, "Shower"),
        GEAR(41, "Gear"),
        SHARPCURVE(42, "SharpCurve"),
        STEEPINCLINE(43, "SteepIncline"),
        TUNNEL(44, "Tunnel"),
        BRIDGE(45, "Bridge"),
        OBSTACLE(46, "Obstacle"),
        CROSSING(47, "Crossing"),
        STORE(48, "Store"),
        TRANSITION(49, "Transition"),
        NAVAID(50, "Navaid"),
        TRANSPORT(51, "Transport"),
        ALERT(52, "Alert"),
        INFO(53, "Info");

        private final int id;


        private final String symbol;

        CoursePoint(int i, String symbol) {
            this.id = i;
            this.symbol = symbol;
        }

        public static CoursePoint fromId(int id) {
            for (CoursePoint symbol : CoursePoint.values()) {
                if (id == symbol.getId()) {
                    return symbol;
                }
            }
            return GENERIC;
        }

        public static CoursePoint fromSymbol(String symbol) {
            for (CoursePoint coursePoint : CoursePoint.values()) {
                if (coursePoint.getSymbol().equals(symbol)) {
                    return coursePoint;
                }
            }
            return GENERIC;
        }

        public int getId() {
            return id;
        }

        public String getSymbol() {
            return symbol;
        }
    }
}
