#!/bin/bash

mongosh --host devicehub-mongo:27017 <<EOF
    rs.initiate({
        _id:'devicehub-rs',
        members: [
            {
                _id:0,
                host:'devicehub-mongo:27017',
                priority: 2
            }
        ]
    })
EOF
