/*
  COPYRIGHT:

    (c) 2007-2022, rapidlasso GmbH - fast tools to catch reality

    This is free software; you can redistribute and/or modify it under the
    terms of the Apache Public License 2.0 published by the Apache Software
    Foundation. See the COPYING file for more information.

    This software is distributed WITHOUT ANY WARRANTY and without even the
    implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
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
