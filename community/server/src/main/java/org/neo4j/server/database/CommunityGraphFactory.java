/*
 * Copyright (c) 2002-2019 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.server.database;

import java.io.File;
import java.util.function.Function;

import org.neo4j.graphdb.facade.GraphDatabaseFacadeFactory;
import org.neo4j.graphdb.facade.GraphDatabaseFacadeFactory.Dependencies;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.graphdb.factory.module.PlatformModule;
import org.neo4j.graphdb.factory.module.edition.AbstractEditionModule;
import org.neo4j.graphdb.factory.module.edition.CommunityEditionModule;
import org.neo4j.kernel.availability.AvailabilityGuard;
import org.neo4j.kernel.availability.AvailabilityGuardInstaller;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.kernel.impl.factory.GraphDatabaseFacade;

import static org.neo4j.kernel.impl.factory.DatabaseInfo.COMMUNITY;

public class CommunityGraphFactory implements GraphFactory
{
    @Override
    public GraphDatabaseFacade newGraphDatabase( Config config, Dependencies dependencies )
    {
        return newGraphDatabase( config, dependencies, availabilityGuard -> {} );
    }

    @Override
    public GraphDatabaseFacade newGraphDatabase( Config config, Dependencies dependencies, AvailabilityGuardInstaller guardInstaller )
    {
        File storeDir = config.get( GraphDatabaseSettings.databases_root_path );

        Function<PlatformModule,AbstractEditionModule> factory = platform ->
        {
            CommunityEditionModule edition = new CommunityEditionModule( platform );
            AvailabilityGuard guard = edition.getGlobalAvailabilityGuard( platform.clock, platform.logging, platform.config );
            guardInstaller.install( guard );
            return edition;
        };

        GraphDatabaseFacadeFactory facadeFactory = new GraphDatabaseFacadeFactory( COMMUNITY, factory );
        return facadeFactory.newFacade( storeDir, config, dependencies );
    }
}
