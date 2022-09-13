package com.github.mreutegg.laszip4j.laszip;

import java.security.InvalidParameterException;
import java.util.ArrayList;

public class LasItemsFactory {
  
    public static LASitem[] getItems(byte pointFormat, char pointRecordSize, char compressorType) {

        ArrayList<LASitem> itemsList = new ArrayList<LASitem>();

        int extraByteSize = 0;
        switch (pointFormat)
        {
        case 0:
            itemsList.add(LASitem.Point10(compressorType==0?0:2));
            extraByteSize = pointRecordSize - 20;
            break;
        case 1:
            itemsList.add(LASitem.Point10(compressorType==0?0:2));
            itemsList.add(LASitem.GpsTime11(compressorType==0?0:2));
            extraByteSize = pointRecordSize - 28;
            break;
        case 2:
            itemsList.add(LASitem.Point10(compressorType==0?0:2));
            itemsList.add(LASitem.Rgb12(compressorType==0?0:2));
            extraByteSize = pointRecordSize - 26;
            break;
        case 3:
            itemsList.add(LASitem.Point10(compressorType==0?0:2));
            itemsList.add(LASitem.GpsTime11(compressorType==0?0:2));
            itemsList.add(LASitem.Rgb12(compressorType==0?0:2));
            extraByteSize = pointRecordSize - 34;
            break;
        case 4:
            itemsList.add(LASitem.Point10(compressorType==0?0:2));
            itemsList.add(LASitem.GpsTime11(compressorType==0?0:2));
            itemsList.add(LASitem.WavePacket13(compressorType==0?0:1));
            extraByteSize = pointRecordSize - 57;
            break;
        case 5:
            itemsList.add(LASitem.Point10(compressorType==0?0:2));
            itemsList.add(LASitem.GpsTime11(compressorType==0?0:2));
            itemsList.add(LASitem.Rgb12(compressorType==0?0:2));
            itemsList.add(LASitem.WavePacket13(compressorType==0?0:1));
            extraByteSize = pointRecordSize - 63;
            break;
        case 6:
            itemsList.add(LASitem.Point14(compressorType==0?0:3));
            extraByteSize = pointRecordSize - 30;
            break;
        case 7:
            itemsList.add(LASitem.Point14(compressorType==0?0:3));
            itemsList.add(LASitem.Rgb14(compressorType==0?0:3));
            extraByteSize = pointRecordSize - 36;
            break;
        case 8:
            itemsList.add(LASitem.Point14(compressorType==0?0:3));
            itemsList.add(LASitem.RgbNIR14(compressorType==0?0:3));
            extraByteSize = pointRecordSize - 38;
            break;
        case 9:
            itemsList.add(LASitem.Point14(compressorType==0?0:3));
            itemsList.add(LASitem.WavePacket14(compressorType==0?0:3));
            extraByteSize = pointRecordSize - 59;
            break;
        case 10:
            itemsList.add(LASitem.Point14(compressorType==0?0:3));
            itemsList.add(LASitem.RgbNIR14(compressorType==0?0:3));
            itemsList.add(LASitem.WavePacket14(compressorType==0?0:3));
            extraByteSize = pointRecordSize - 67;
            break;
        default:
          throw new InvalidParameterException("Unknown point format");
        }
      
        if ( extraByteSize > 0 )
        {
            if (pointFormat < 6)
                itemsList.add(LASitem.ExtraBytes(extraByteSize, compressorType==0?0:(pointFormat>5?3:2)));
            else
                itemsList.add(LASitem.ExtraBytes14(extraByteSize, compressorType==0?0:(pointFormat>5?3:2)));
        }

        LASitem result[] = new LASitem[itemsList.size()];
        result = itemsList.toArray(result);

        return result;      
    }
}
