import threading
import time
import requests
from datetime import datetime
import pandas as pd
import pyshark

def packet_capture(url_name, capture_duration):
    capture = pyshark.LiveCapture(interface='eth0')
    packets_data = []
    
    start_time = time.time()  # Record the start time
    
    try:
        for pkt in capture.sniff_continuously():
            current_time = time.time()
            elapsed_time = current_time - start_time

            if elapsed_time > capture_duration:
                break  # Stop the capture after the specified duration
            if 'IP' in pkt:
                packet_info = {}

                # Gather basic IP information
                ip_layer = pkt['IP']
                packet_info['source_ip'] = ip_layer.src
                packet_info['destination_ip'] = ip_layer.dst
                packet_info['protocol'] = ip_layer.proto

                #if ip_layer.proto=='17':
                 #   print(pkt)
                packet_info['top_layer'] = pkt.highest_layer
                packet_info['packet_length'] = pkt.length
                packet_info['packet_number'] = pkt.number
                packet_info['capture_time'] = pkt.sniff_time
                packet_info['timestamp'] = pkt.sniff_timestamp

                # Process DNS information if available
                if 'DNS' in pkt:
                    dns_layer = pkt['DNS']
                    packet_info['dns_query'] = dns_layer.qry_name if hasattr(dns_layer, 'qry_name') else ""
                    packet_info['dns_response'] = dns_layer.resp_name if hasattr(dns_layer, 'resp_name') else ""
                    packet_info['dns_type'] = dns_layer.qry_type if hasattr(dns_layer, 'qry_type') else ""
                    packet_info['dns_class'] = dns_layer.qry_class if hasattr(dns_layer, 'qry_class') else ""
                    packet_info['query_count'] = dns_layer.count_queries
                    packet_info['answer_count'] = dns_layer.count_answers
                    packet_info['authority_count'] = dns_layer.count_auth_rr
                    #packets_data.append(packet_info)


                # Include TCP details if present
                if 'TCP' in pkt:
                    tcp_layer = pkt['TCP']
                    packet_info['source_port'] = tcp_layer.srcport
                    packet_info['destination_port'] = tcp_layer.dstport
                    packet_info['tcp_flags'] = tcp_layer.flags
                    packet_info['sequence_number'] = tcp_layer.seq
                    packet_info['acknowledgement'] = tcp_layer.ack
                    #packets_data.append(packet_info)

                 # Include UDP details if present
                if 'UDP' in pkt:
                    udp_layer = pkt['UDP']
                    packet_info['udp_source_port'] = udp_layer.srcport
                    packet_info['udp_destination_port'] = udp_layer.dstport
                    #packets_data.append(packet_info)

                # Handle TLS encrypted information
                if 'TLS' in pkt:
                    # Omitting direct TLS details for simplicity
                    tls_layer=pkt['TLS']
                    packet_info["tls"]="TLS_Packet_Captured"
                    #print(packet_info, "TLS packet captured")

                packets_data.append(packet_info)
                    # Convert to DataFrame and save after adding TLS packet info
    finally:
        # Always ensure the capture is properly closed
        capture.close()
        pd.DataFrame(packets_data).to_csv(f"capture_{url_name}.csv")
    

def make_requests_and_log(url, url_name, num_requests=3):
    records = []
    for i in range(num_requests):
        now = datetime.now()
        timestamp = datetime.timestamp(now)
        try:
            response = requests.get(url)
            response.raise_for_status()
            records.append({"index": i, "time": now, "timestamp": timestamp, "status": "success"})
        except requests.RequestException as e:
            print(f"Request failed for {url_name} at index {i}: {e}")
            records.append({"index": i, "time": now, "timestamp": timestamp, "status": "failed"})
    
    df = pd.DataFrame(records)
    df.to_csv(f"{url_name}_request_records.csv")

urls = [
        ("https://www.indiatimes.com", "indiatimes"),
        ("https://www.washingtonpost.com", "washingtonpost"),
        ("https://www.ndtv.com","ndtv"),
        ("https://www.cnbc.com","cnbc"),
        ("https://www.timesofindia.com","timesofindia"),
        ("https://www.express.co.uk","express_uk"),
        ("https://www.rt.com","rt"),
        ("https://www.news18.com","news18"),
        ("https://www.nypost.com","nypost"),
        ("https://www.abc.net.au","abc"),
        ("https://www.bbc.co.uk","bbc_uk"),
        ("https://www.msn.com","msn"),
        ("https://www.cnn.com","cnn"),
        ("https://www.news.google.com","google_news"),
        ("https://www.dailymail.co.uk","dailymail"),
        ("https://www.nytimes.com","nytimes"),
        ("https://www.theguardian.com","theguardian"),
        ("https://www.foxnews.com","foxnews"),
        ("https://www.finance.yahoo.com","finance_yahoo"),
        ("https://www.news.yahoo.com","news_yahoo")
]

for url, name in urls:
    capture_thread = threading.Thread(target=packet_capture, args=(name, 30))
    capture_thread.start()
    print(name)
    # Introducing a small delay to ensure capturing starts before requests
    time.sleep(1)
    
    make_requests_and_log(url, name, num_requests=3)
    
    capture_thread.join()