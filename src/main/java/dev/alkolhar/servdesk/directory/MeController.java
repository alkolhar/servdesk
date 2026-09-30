package dev.alkolhar.servdesk.directory;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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

	public MeController(PersonQueryService queryService) {
		this.queryService = queryService;
	}

	@GetMapping
	public MeModel me(Authentication authentication) {
		Person caller = ((PersonUserDetails) authentication.getPrincipal()).getPerson();
		Person person = queryService.findById(caller.getId());
		MeModel model = new MeModel();
		model.setId(person.getId());
		model.setRole(person.getRole());
		model.setName(person.getName());
		model.setEmail(person.getEmail());
		model.setUsername(person.getUsername());
		model.add(linkTo(MeController.class).withSelfRel());
		return model;
	}

}
