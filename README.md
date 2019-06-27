# PlexGap
Find missing entries from movie series in your Plex library.

For example: You have Iron Man 1 and Iron Man 3, this will inform you that Iron Man 2 is missing.

## Credentials

To get started, edit credentials.json and provide the required credentials.

### Plex token

A temporary Plex token can be found by right clicking a movie in your library, selecting `Get Info` from the drop down, and then clicking `View XML`.

The token is located in the address bar as `X-Plex-Token=YOURTOKENHERE`, only the `YOURTOKENHERE` is required.

### Plex IP

The IP address that your Plex server runs on including port. E.g: `192.168.1.138:32400`

### TMDB API Key

PlexGap utelises the TMDB API to find out whether a movie is part of a series, and to find the other members of these series.

To obtain an API key for TMDB, create a free account and verify your email.

Navigate to the API settings by clicking your account icon in the top right, selecting `settings` from the drop down and then `API` on the left dashboard.

Click to generate a new API key as a developer and accept the terms and conditions.

You will now be asked to input information about the application that will use this API key, anything can be entered here.

Once done, your API key will be located under `API Key (v3 auth)`

## Running the application

When running the application you will be asked to select an option: 

```
Where would you like to read your Plex library in from?

1. A JSON file 

2. The Plex API
```

### JSON file

As obtaining the information for all movies in your plex library is a time consuming process, the application will save the relevant 
data in a json file as it is found.

If you have run the application before and haven't added anything new to your library, you can opt to provide the path (including extension) to this json file
to prevent having to go through the process again.

### Plex API

You will be prompted to provide a path for where the application should save the data that is found in json format.

E.g: `C:Users\Me\Desktop\movies.json` or `movies.json` etc.

This file will be created, and can be used to quickly run the application again without going through the below process.

The application will first query your plex server for the contents of your library.

Each item has a unique `ratingKey` which the application must then pass back to plex to obtain the item's metadata.

This metadata includes the `guid` of the item, which is the unique id used by either IMDB or TMDB to identify a movie.

The `guid` is then passed to the TMDB API to find the collection id of the movie (if it belongs to a collection).

## Using the data

Once the data has been collected, you will be prompted to select an option:

```
What would you like to do with your movies?

1. Find missing sequels
```

### Find missing sequels

The application will go through all items which belong to a collection and query the TMDB API with the collection id
(if it has not done so already for a previous item). 

This will provide a list of items which belong to the collection.

As items belonging to the same collection are found, they are marked as seen in this list.

This process does not take too long as only one API call is made per collection.

Upon checking all movies, a breakdown of missing items is provided, seperated by collection:

```
X-Men Collection:
Dark Phoenix 

John Wick Collection:
John Wick: Chapter 3 – Parabellum

Jurassic Park Collection:
The Lost World: Jurassic Park 
Jurassic Park III 
Jurassic Park 
```




