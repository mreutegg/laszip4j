package com.github.mreutegg.laszip4j.laszip;

class LAScontextPOINT14
{
  public boolean unused;

  public PointDataRecordPoint14 last_item = null;
  public int[] last_intensity = new int[8];
  public StreamingMedian5[] last_X_diff_median5 = StreamingMedian5.newStreamingMedian5(12);
  public StreamingMedian5[] last_Y_diff_median5 = StreamingMedian5.newStreamingMedian5(12);
  public long[] last_Z = new long[8];

  public boolean initialized = false;
  public ArithmeticModel[] m_changed_values = new ArithmeticModel[8];
  public ArithmeticModel m_scanner_channel;
  public ArithmeticModel[] m_number_of_returns = new ArithmeticModel[16];
  public ArithmeticModel m_return_number_gps_same;
  public ArithmeticModel[] m_return_number = new ArithmeticModel[16];
  public IntegerCompressor ic_dX;
  public IntegerCompressor ic_dY;
  public IntegerCompressor ic_Z;

  public ArithmeticModel[] m_classification = new ArithmeticModel[64];

  public ArithmeticModel[] m_flags = new ArithmeticModel[64];

  public ArithmeticModel[] m_user_data = new ArithmeticModel[64];

  public IntegerCompressor ic_intensity;

  public IntegerCompressor ic_scan_angle;

  public IntegerCompressor ic_point_source_ID;

  // GPS time stuff
  public int last, next;
  public long[] last_gpstime = new long[4];
  public int[] last_gpstime_diff = new int[4];
  public int[] multi_extreme_counter = new int[4];

  public ArithmeticModel m_gpstime_multi;
  public ArithmeticModel m_gpstime_0diff;
  public IntegerCompressor ic_gpstime;
}
