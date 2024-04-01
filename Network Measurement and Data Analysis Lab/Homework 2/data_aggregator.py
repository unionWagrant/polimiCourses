
import pandas as pd
#my_ip eth0: flags=4163<UP,BROADCAST,RUNNING,MULTICAST>  mtu 1500 inet 192.168.159.238  netmask 255.255.240.0  broadcast 192.168.159.255

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


dfs = []

# Loop through the CSV files and their labels
for url, label in urls:
    # Read the current CSV file into a DataFrame
    filepath="/mnt/c/Users/fatih.temiz/TrafficClassification/Dataset_3requests/"+f"capture_{label}.csv"
    df = pd.read_csv(filepath)
    
    df['label'] = label
    
    # Append the DataFrame to the list
    dfs.append(df)

# Concatenate all DataFrames in the list into one
final_df = pd.concat(dfs, ignore_index=True)

# Save the aggregated DataFrame to a new CSV file
aggregated_data_path="/mnt/c/Users/fatih.temiz/TrafficClassification/aggregated_data_with_udp_corrected_with_3_requests.csv"
final_df.to_csv(aggregated_data_path, index=False)



