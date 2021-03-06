/*
 * Copyright (c) 2013 Atlanmod.
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v2.0 which accompanies
 * this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 */

package fr.inria.atlanmod.neoemf.eclipse.ui.command;

import fr.inria.atlanmod.neoemf.config.Config;
import fr.inria.atlanmod.neoemf.data.InvalidBackendException;
import fr.inria.atlanmod.neoemf.eclipse.ui.NeoUIPlugin;
import fr.inria.atlanmod.neoemf.eclipse.ui.editor.NeoEditor;
import fr.inria.atlanmod.neoemf.util.UriFactory;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.ui.URIEditorInput;
import org.eclipse.emf.common.util.URI;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.progress.UIJob;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A {@link org.eclipse.core.commands.IHandler} that opens an existing {@code Backend}.
 */
public class OpenBackendCommand extends AbstractSelectionSingleHandler<IFolder> {

    public OpenBackendCommand() {
        super(IFolder.class);
    }

    @Override
    protected void execute(ExecutionEvent event, IFolder selectedFolder) throws ExecutionException {
        final IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
        new OpenBackendJob(window.getShell().getDisplay(), selectedFolder).schedule();
    }

    /**
     * A {@link org.eclipse.core.runtime.jobs.Job} that opens the {@link NeoEditor} on an existing {@code Backend}.
     */
    private class OpenBackendJob extends UIJob {

        private final IFolder directory;

        /**
         * Constructs a new {@code OpenBackendJob} with the given {@code display}.
         *
         * @param display the display to execute the asyncExec in
         */
        public OpenBackendJob(Display display, IFolder directory) {
            super(display, "Opening Model Database");

            this.directory = directory;
        }

        @Override
        public IStatus runInUIThread(IProgressMonitor monitor) {
            Path root = Paths.get(directory.getRawLocation().toOSString());

            try {
                URI uri = getUriFactory(root).createLocalUri(root.toFile());

                PlatformUI.getWorkbench()
                        .getActiveWorkbenchWindow()
                        .getActivePage()
                        .openEditor(new URIEditorInput(uri), NeoEditor.EDITOR_ID);
            }
            catch (Exception e) {
                return new Status(IStatus.ERROR, NeoUIPlugin.PLUGIN_ID, "Unable to open editor", e);
            }

            return Status.OK_STATUS;
        }

        /**
         * Retrieves the {@link UriFactory} to use from the NeoEMF configuration in the given {@code directory}.
         *
         * @param directory the directory where to find the configuration
         *
         * @return the {@link UriFactory}
         *
         * @throws IOException             if the {@code directory} does not have configuration file
         * @throws InvalidBackendException if the information stored in the configuration does not permit to retrieve
         *                                 the {@link UriFactory}
         */
        private UriFactory getUriFactory(Path directory) throws IOException {
            return Config.load(directory)
                    .map(Config::getName)
                    .map(UriFactory::forName)
                    .orElseThrow(() -> new FileNotFoundException(String.format("Unable to find the configuration from %s", directory)));
        }
    }
}
