# Movielens dataset in neo4j #

get the dataset from the movilens page :
        wget http://www.grouplens.org/system/files/ml-100k.zip

Imported as a graph:
* user node (property: {age, gender})
* movie node (property: {name, date})
* genre node (linked to movie)
* occupation node (linked to user)

For simplicity the rating will be transformed to a like/dislike relationship.
like if rating > 2 else dislike.