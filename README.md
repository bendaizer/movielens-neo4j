# Movielens dataset in neo4j #

Needs a lib/ directory with neo4j-community-1.9.RC1 unzipped inside
http://www.neo4j.org/download

Dataset from the movilens page :
        wget http://www.grouplens.org/system/files/ml-100k.zip
The data are expected in the ml-100k/ directory

Imported as a graph:
* user node (with properties as in u.user)
* movie node (with properties as in u.item)
* genre node (linked to movie)
* occupation node (linked to user)

For simplicity the rating will be transformed to a like/dislike relationship.
LIKE if rating > 2 else DISLIKE.

To use it
        chmod +x run.sh
        ./run.sh