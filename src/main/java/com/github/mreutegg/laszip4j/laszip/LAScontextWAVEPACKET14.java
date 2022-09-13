package com.github.mreutegg.laszip4j.laszip;

class LAScontextWAVEPACKET14
{
  public boolean unused;

  public PointDataRecordWavepacket last_item;
  public int last_diff_32;
  public int sym_last_offset_diff;

  public ArithmeticModel m_packet_index;
  public ArithmeticModel[] m_offset_diff = new ArithmeticModel[4];
  public IntegerCompressor ic_offset_diff;
  public IntegerCompressor ic_packet_size;
  public IntegerCompressor ic_return_point;
  public IntegerCompressor ic_xyz;
}
