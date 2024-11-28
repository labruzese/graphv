package com.fischerabruzese.graphsFX.machines

import com.fischerabruzese.graphsFX.FXVertex

interface IPathMachine<E> {
    var currentPathVerticies: List<FXVertex<E>>
    var currentPathConnections: List<Connection<E>>
}