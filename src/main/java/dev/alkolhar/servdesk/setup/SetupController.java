package dev.alkolhar.servdesk.setup;

import dev.alkolhar.servdesk.directory.PersonCommandService;
import dev.alkolhar.servdesk.directory.PersonModel;
import dev.alkolhar.servdesk.directory.PersonModelAssembler;
import dev.alkolhar.servdesk.directory.PersonQueryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * First-run bootstrap: an empty database has no
 * {@link dev.alkolhar.servdesk.directory.Person} to authenticate as, so nothing
 * else in the API is reachable until one exists. This endpoint is reachable
 * without authentication (see SecurityConfig) specifically to break that
 * chicken-and-egg problem, but {@link PersonCommandService#createInitialAgent}
 * refuses to run a second time once any Person exists, so it can't be used to
 * mint extra accounts after setup is done.
 */
@RestController
@RequestMapping("/api/setup")
public class SetupController {

	private final PersonCommandService commandService;
	private final PersonQueryService queryService;
	private final PersonModelAssembler assembler;

	public SetupController(PersonCommandService commandService, PersonQueryService queryService,
			PersonModelAssembler assembler) {
		this.commandService = commandService;
		this.queryService = queryService;
		this.assembler = assembler;
	}

	@GetMapping
	public SetupStatus status() {
		return new SetupStatus(queryService.isSetupRequired());
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public PersonModel createInitialAgent(@Valid @RequestBody SetupRequest request) {
		return assembler.toModel(commandService.createInitialAgent(request.name(), request.email(), request.phone(),
				request.username(), request.password()));
	}
}
