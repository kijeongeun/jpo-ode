package us.dot.its.jpo.ode.plugin.j2735.builders;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Test class for TravelerMessageFromHumanToAsnConverter.translateISOTimeStampToMinuteOfYear()
 *
 */
public class TimeStampConverterTest {
   
   @Test
   public void test1() {
      String testInput = "2017-11-20T22:16:12.874Z";
      
      int expectedResult = 0;
   }
   
   /**
    * Any invalid time should return 527040
    */
   @Test
   public void testInvalidFlag() {
      String invalidTime = "2017-11-20T22_invalid_time_:16:12.874Z";
      long expectedResult = 527040;
      
      long actualResult = TravelerMessageFromHumanToAsnConverter.translateISOTimeStampToMinuteOfYear(invalidTime);
      
      assertEquals(expectedResult, actualResult);  
   }
   
   /**
    * Earliest time in the year should return minimum value
    */
   @Test
   public void testLowerBound() {
      String testInput = "2017-1-00T00:00:00.000Z";
      long expectedResult = 0;
      
      long actualResult = TravelerMessageFromHumanToAsnConverter.translateISOTimeStampToMinuteOfYear(testInput);
      
      assertEquals(expectedResult, actualResult);  
   }
   
   /**
    * Latest time in the year should return max value
    */
   @Test
   public void testUpperBound() {
      String testInput = "2017-1-00T00:00:00.000Z";
      long expectedResult = 527039;
      
      long actualResult = TravelerMessageFromHumanToAsnConverter.translateISOTimeStampToMinuteOfYear(testInput);
      
      assertEquals(expectedResult, actualResult);  
   }
   
   

}
