package dev.alkolhar.servdesk.directory;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code GET /api/me}: the SPA's startup call — a 401 here sends it to its
 * login screen (ADR-0005). Works the same for a session and for HTTP Basic.
 * <p>
 * Reloads the person rather than echoing the one cached in the session's
 * {@code Authentication}: that copy is a snapshot from login time, and a rename
 * or role change since then should show up on the next page load.
 */
@RestController
@RequestMapping(value = "/api/me", version = "1")
public class MeController {

	private final PersonQueryService queryService;
	private final PersonCommandService commandService;

	public MeController(PersonQueryService queryService, PersonCommandService commandService) {
		this.queryService = queryService;
		this.commandService = commandService;
	}

	@GetMapping
	public MeModel me(Authentication authentication) {
		Person person = queryService.findById(callerId(authentication));
		MeModel model = new MeModel();
		model.setId(person.getId());
		model.setRole(person.getRole());
		model.setName(person.getName());
		model.setEmail(person.getEmail());
		model.setUsername(person.getUsername());
		model.add(linkTo(MeController.class).withSelfRel());
		return model;
	}

	/**
	 * Your own password, any logged-in person (#89). 204 on success; 403 when
	 * {@code currentPassword} doesn't match (see
	 * {@link PersonCommandService#changeOwnPassword}).
	 */
	@PutMapping("/password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void changePassword(Authentication authentication, @Valid @RequestBody ChangePasswordRequest request) {
		commandService.changeOwnPassword(callerId(authentication), request.currentPassword(), request.newPassword());
	}

	private static Long callerId(Authentication authentication) {
		return ((PersonUserDetails) authentication.getPrincipal()).getPerson().getId();
	}

}
