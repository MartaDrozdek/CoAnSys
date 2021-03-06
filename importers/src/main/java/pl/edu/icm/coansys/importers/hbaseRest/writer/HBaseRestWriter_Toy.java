/*
 * (C) 2010-2012 ICM UW. All rights reserved.
 */

package pl.edu.icm.coansys.importers.hbaseRest.writer;

import java.io.IOException;

import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.rest.client.Client;
import org.apache.hadoop.hbase.rest.client.Cluster;
import org.apache.hadoop.hbase.rest.client.RemoteHTable;
import org.apache.hadoop.hbase.util.Bytes;


import pl.edu.icm.coansys.importers.constants.HBaseConstant;
import pl.edu.icm.coansys.importers.hbaseRest.model_toy.AddressBookProtos.Person;
import pl.edu.icm.coansys.importers.iterators.ZipDirToDocumentDTOIterator;
import pl.edu.icm.coansys.importers.model.DocumentDTO;
import pl.edu.icm.coansys.importers.transformer.DocumentDto2HBasePut;

/**
 * 
 * @author pdendek
 *
 */
public class HBaseRestWriter_Toy {
	
	public static void main(String[] args) throws IOException{

		
		RemoteHTable table = new RemoteHTable(
        		new Client(
        				new Cluster().add("localhost", 8080)
        		), "test"
        	);
		
        Person john =
                Person.newBuilder().setId(1234).setName("John Doe").setEmail("jdoe@example.com").addPhone(
                Person.PhoneNumber.newBuilder().setNumber("555-4321").setType(Person.PhoneType.HOME)).build();

        byte[] row = Bytes.toBytes("row3");
        table.delete(new Delete(row));
        
        Put put = new Put(row);
        byte[] family = Bytes.toBytes("data");
        byte[] qualifier = Bytes.toBytes("3");
        byte[] value = john.toByteArray(); 
        
        put.add(family, qualifier, value);
        table.put(put);		
	}
}
