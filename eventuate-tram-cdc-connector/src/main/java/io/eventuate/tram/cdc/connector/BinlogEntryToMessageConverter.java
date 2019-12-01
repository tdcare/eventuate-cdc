package io.eventuate.tram.cdc.connector;

import io.eventuate.common.eventuate.local.BinlogFileOffset;
import io.eventuate.common.json.mapper.JSonMapper;
import io.eventuate.local.common.BinlogEntry;
import io.eventuate.local.common.BinlogEntryToEventConverter;

import java.io.UnsupportedEncodingException;
import java.util.Map;

public class BinlogEntryToMessageConverter implements BinlogEntryToEventConverter<MessageWithDestination> {
  @Override
  public MessageWithDestination convert(BinlogEntry binlogEntry)  {
    String destination=null;
    String payload=null;
    Map<String,String> headers=null;
    BinlogFileOffset binlogFileOffset=null;

    destination=(String)binlogEntry.getColumn("destination");
    Object payloadColumn=binlogEntry.getColumn("payload");
   try {
    if (payloadColumn instanceof byte[]){
      payload=new String((byte[]) payloadColumn,"UTF-8");
    }else {
    payload= (String) payloadColumn;
    }
   }catch (Exception e){}

    headers=JSonMapper.fromJson((String)binlogEntry.getColumn("headers"), Map.class);
    binlogFileOffset=binlogEntry.getBinlogFileOffset();

    return new MessageWithDestination(destination, payload, headers, binlogFileOffset);
  }
}
