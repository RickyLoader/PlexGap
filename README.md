# PlexGap
Find missing entries from movie series in your Plex library.

For example: You have Iron Man 1 and Iron Man 3, this will inform you that Iron Man 2 is missing.

## Credentials

To get started, edit credentials.json and provide the required credentials.

### Plex token & Library ID

A temporary Plex token can be found by clicking the 3 dot icon on a movie in your library, selecting `Get Info` from the drop down, and then clicking `View XML`.

The token is located in the address bar as `X-Plex-Token=YOUR_TOKEN_HERE`, only `YOUR_TOKEN_HERE` is required.

The library id is found in the XML as `librarysectionID="x"`, only `x` is required. 

### Plex IP

The IP address that your Plex server runs on including port. E.g: `192.168.1.138:32400`

### TMDB API Key & TMDB Read Access Token

PlexGap utelises the TMDB API to find out whether a movie is part of a series. 

The read acccess token allows temporary write access to create a list on your TMDB profile containing the movies that were found.

To obtain an API key for TMDB, create a free account and verify your email.

Navigate to the API settings by clicking your account icon in the top right, selecting `settings` from the drop down and then `API` on the left dashboard.

Click to generate a new API key as a developer and accept the terms and conditions.

You will now be asked to input information about the application that will use this API key, anything can be entered here.

Once done, your API key will be located under `API Key (v3 auth)` and your read access token under `API Read Access Token (v4 auth)`.

## Running the application
Make sure credentials.json and the jar file are in the same directory and type `java -jar PlexGap.jar` in CMD.


When running the application you will be asked to select an option: 

```
Where would you like to read your Plex library in from?

1. [json_file].json 

2. The Plex API
```

### JSON file

As obtaining the information for all movies in your plex library is a time consuming process, the application will save the relevant 
data in a json file as it is found.

If you have run the application before, you can select this option to prevent having to go through the process again. New movies on plex that are not in the json file are appended to it.

### Plex API

The application will first query your plex server for the contents of your library.

Each item in your library has a unique `guid`, which is the id used by either IMDB or TMDB to identify a movie.

This `guid` is passed to the TMDB API to find the collection id of the movie (if it belongs to a collection).

This process can take a few minutes depending on the size of your library, so the results are stored in a json file named what has been specified in credentials.json. This can be used to quickly run the application again.

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

This process does not take long as only one API call is made per collection.

Upon checking all movies, you will be prompted with a link to follow:

```
Please visit:

https://www.themoviedb.org/auth/access?request_token={YOUR_REQUEST_TOKEN_HERE}

To approve the application.

This allows it to create a TMDB list containing your missing movies.

Type "ok" when ready:
```

This is due to TMDB requiring multi part authentication in allowing applications to write to your profile.

First the application passes your provided `read access token` to obtain a `request token`. 

You must then follow the provided link to approve this request token, and confirm to the application that this has been done.

This allows the application to use the now `approved request token` to obtain an `access token`, finally granting temporary permission to write data to your profile.

Once you have approved this request and typed `ok`, you will be asked to provide a name for your list, and then be directed to the newly created TMDB list containing all of the missing movies that were found:

```
Your list has been created!

Visit:

https://www.themoviedb.org/list/{NEW_LIST_ID}
```
## Note

TMDB collections are curated but can be quite broad/generalised - There is a singular James Bond collection, not one per actor. As this list is created on your profile, you can remove any movies you don't want and import it in to Radarr.
