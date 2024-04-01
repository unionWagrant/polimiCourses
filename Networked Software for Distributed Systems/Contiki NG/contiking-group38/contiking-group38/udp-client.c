#include "contiki.h"
#include "net/routing/routing.h"
#include "random.h"
#include "net/netstack.h"
#include "net/ipv6/simple-udp.h"

#include "sys/log.h"
#define LOG_MODULE "App"
#define LOG_LEVEL LOG_LEVEL_INFO

#define WITH_SERVER_REPLY  1
#define UDP_CLIENT_PORT 8765
#define UDP_SERVER_PORT 5678

static struct simple_udp_connection udp_conn;

#define MAX_READINGS 10
#define SEND_INTERVAL (60 * CLOCK_SECOND)
#define FAKE_TEMPS 5

static struct simple_udp_connection udp_conn;

/*---------------------------------------------------------------------------*/
PROCESS(udp_client_process, "UDP client");
AUTOSTART_PROCESSES(&udp_client_process);
/*---------------------------------------------------------------------------*/
static float
get_temperature()
{
  static float fake_temps [FAKE_TEMPS] = {30, 25, 20, 15, 10};
  return fake_temps[random_rand() % FAKE_TEMPS];
  
}
/*---------------------------------------------------------------------------*/
static void
udp_rx_callback(struct simple_udp_connection *c,
         const uip_ipaddr_t *sender_addr,
         uint16_t sender_port,
         const uip_ipaddr_t *receiver_addr,
         uint16_t receiver_port,
         const uint8_t *data,
         uint16_t datalen)
{
  LOG_INFO("Received init response" );
}
/*---------------------------------------------------------------------------*/
PROCESS_THREAD(udp_client_process, ev, data)
{
  // Initializing timers udp vars
  static struct etimer et;
  uip_ipaddr_t dst_ipaddr;
  // how many times it could not set the average temperature
  static unsigned int retry_counter = 0;
  // total average until now
  static float batched_average = 0;
  static float readingsWhileDisconnected[MAX_READINGS];

  PROCESS_BEGIN();

  /* Initialize UDP connection */
  simple_udp_register(&udp_conn, UDP_CLIENT_PORT, NULL,
                      UDP_SERVER_PORT, udp_rx_callback);


  etimer_set(&et, SEND_INTERVAL);

  while(1) {
    PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&et));
     //etimer_reset(&et);
    // Get the temperature
    float temperature=get_temperature();
    // Check if we can send or not, if not we add to the counter and average the temperature
    if(NETSTACK_ROUTING.node_is_reachable() && NETSTACK_ROUTING.get_root_ipaddr(&dst_ipaddr)) {
      if (retry_counter > 0) {
        for (unsigned i=0; i<MAX_READINGS; i++) {
          if (readingsWhileDisconnected[i]){
            batched_average += readingsWhileDisconnected[i];
          }
        }
        if(retry_counter > MAX_READINGS) {
          batched_average = batched_average / MAX_READINGS;
        } else {
          batched_average = batched_average / retry_counter;
        }
        LOG_INFO("Sending temperature %f to ", temperature);
        LOG_INFO_6ADDR(&dst_ipaddr);
        LOG_INFO_("\n");
        simple_udp_sendto(&udp_conn, &temperature, sizeof(batched_average), &dst_ipaddr);
        // reset the value of sending queue
        for (unsigned i=0; i<MAX_READINGS; i++) {
          readingsWhileDisconnected[i] = 0;
        }
        batched_average = 0;
        retry_counter = 0;
      }
      // sending the temp
      LOG_INFO("Sending temperature %f to ", temperature);
      LOG_INFO_6ADDR(&dst_ipaddr);
      LOG_INFO_("\n");
      simple_udp_sendto(&udp_conn, &temperature, sizeof(temperature), &dst_ipaddr);
    } else {
      LOG_INFO("Not reachable yet\n");
      // calculating the average
      if(retry_counter == 0) {
        readingsWhileDisconnected[0] = temperature;
        retry_counter++;
      }
      else {
        // replacing the array if we have more than MAX_READINGS
        readingsWhileDisconnected[(retry_counter % MAX_READINGS)] = temperature;
        retry_counter++;
      }
    }
    etimer_reset(&et);
  }

  PROCESS_END();
}
/*---------------------------------------------------------------------------*/