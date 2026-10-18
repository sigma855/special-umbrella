# DonutMarket (Paper 26.2 plugin)

Requires: Paper 26.2+, Java 25 on the server.

Commands
- /ah                         open the Auction House
- /ah sell <price>            list the item in your main hand (whole stack), 48h, max 10 listings
- /order                      open Buy Orders
- /order create <item> <amount> <price each>    e.g. /order create diamond 64 1.5k
- /bal (/balance, /money)     check money
- /pay <player> <amount>      send money (supports k / m / b)
- /eco give|take|set <player> <amount>          needs donutmarket.admin (ops by default)

Orders: money for the whole order is held in escrow. Anyone can open /order, click an order, and
plain matching items from their inventory are delivered and paid instantly. Items go to the buyer
(or wait for them until they log in). Cancelling refunds the unfilled part.

Build (needs JDK 25 + Gradle 9.1 or newer):  gradle build
Jar: build/libs/donutmarket-1.0.0.jar  -> put it in your server's plugins/ folder.
Data: plugins/DonutMarket/data.yml
Settings (start money, limits): top of Market.java

Ranks (colors shown in chat, tab list and above heads)
- Owner = aqua, Admin = red, Media = pink, Member = grey (default for everyone)
- /rank list | /rank get <player> | /rank set <player> <owner|admin|media|member>
- Who can set ranks: server console, ops, or anyone with the Owner rank.
- FIRST TIME: type this in the server console to make yourself owner:   rank set YourName owner
