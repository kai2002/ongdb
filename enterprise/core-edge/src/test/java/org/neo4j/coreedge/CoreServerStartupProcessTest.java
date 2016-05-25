/*
 * Copyright (c) 2002-2016 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.coreedge;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;

import java.util.List;

import org.neo4j.coreedge.catchup.CatchupServer;
import org.neo4j.coreedge.raft.DelayedRenewableTimeoutService;
import org.neo4j.coreedge.raft.RaftInstance;
import org.neo4j.coreedge.raft.RaftServer;
import org.neo4j.coreedge.raft.membership.MembershipWaiter;
import org.neo4j.coreedge.raft.replication.id.ReplicatedIdGeneratorFactory;
import org.neo4j.coreedge.raft.state.CoreState;
import org.neo4j.coreedge.server.CoreMember;
import org.neo4j.coreedge.server.core.CoreServerStartupProcess;
import org.neo4j.helpers.collection.Iterables;
import org.neo4j.kernel.impl.transaction.state.DataSourceManager;
import org.neo4j.kernel.lifecycle.LifeSupport;
import org.neo4j.kernel.lifecycle.Lifecycle;
import org.neo4j.logging.NullLogProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.neo4j.coreedge.CoreServerStartupProcessTest.LifeSupportMatcherBuilder.startsComponent;

public class CoreServerStartupProcessTest
{
    @Test
    public void raftTimeOutServiceTriggersMessagesSentToAnotherServer() throws Exception
    {
        DataSourceManager dataSourceManager = mock( DataSourceManager.class );
        ReplicatedIdGeneratorFactory idGeneratorFactory = mock( ReplicatedIdGeneratorFactory.class );
        RaftServer<CoreMember> raftServer = mock( RaftServer.class );
        CatchupServer catchupServer = mock( CatchupServer.class );
        DelayedRenewableTimeoutService raftTimeoutService = mock( DelayedRenewableTimeoutService.class );
        MembershipWaiter<CoreMember> membershipWaiter = mock( MembershipWaiter.class );
        RaftInstance raftInstance = mock( RaftInstance.class );
        CoreState recoverableStateMachine = mock( CoreState.class );

        LifeSupport lifeSupport = CoreServerStartupProcess.createLifeSupport( dataSourceManager,
                idGeneratorFactory, raftInstance, recoverableStateMachine, raftServer, catchupServer, raftTimeoutService,
                membershipWaiter, 0, NullLogProvider.getInstance() );

        assertThat( lifeSupport, startsComponent( raftTimeoutService ).after( raftServer )
                .because( "server need to be ready to handle responses generated by timeout events" ) );

        assertThat( lifeSupport, startsComponent( raftTimeoutService ).after( recoverableStateMachine )
                .because( "elections which must request votes from the latest known voting members" ) );

        assertThat( lifeSupport, startsComponent( recoverableStateMachine ).after( dataSourceManager )
                .because( "transactions are replayed from the RAFT log into the data source" ) );

        assertThat( lifeSupport, startsComponent( idGeneratorFactory ).after( dataSourceManager )
                .because( "IDs are generated into the data source" ) );
    }

    static class LifeSupportMatcher extends TypeSafeMatcher<LifeSupport>
    {
        private final Lifecycle component1;
        private final Lifecycle component2;
        private final String reason;

        public LifeSupportMatcher( Lifecycle component1, Lifecycle component2, String reason )
        {
            this.component1 = component1;
            this.component2 = component2;
            this.reason = reason;
        }

        @Override
        protected boolean matchesSafely( LifeSupport lifeSupport )
        {
            List<Lifecycle> lifeCycles = Iterables.asList( lifeSupport.getLifecycleInstances() );
            return lifeCycles.indexOf( component2 ) < lifeCycles.indexOf( component1 );
        }

        @Override
        public void describeTo( Description description )
        {
            description.appendText( component1.toString().replaceAll( "Mock for (.*), hashCode.*", "$1" ) );
            description.appendText( " starts after " );
            description.appendText( component2.toString().replaceAll( "Mock for (.*), hashCode.*", "$1" ) );
            description.appendText( " because " + this.reason );
        }
    }

    static class LifeSupportMatcherBuilder
    {
        private Lifecycle component1;
        private Lifecycle component2;

        public static LifeSupportMatcherBuilder startsComponent( Lifecycle component )
        {
            LifeSupportMatcherBuilder builder = new LifeSupportMatcherBuilder();
            builder.component1 = component;
            return builder;
        }

        public LifeSupportMatcherBuilder after( Lifecycle component2 )
        {
            this.component2 = component2;
            return this;
        }

        public LifeSupportMatcher because( String reason )
        {
            return new LifeSupportMatcher( component1, component2, reason );
        }
    }
}
