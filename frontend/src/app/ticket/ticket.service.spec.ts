import { actionsOf, pathOf } from './ticket.service';

describe('ticket links', () => {
  it('turns an absolute HAL href into a same-origin path, so the XSRF header is sent', () => {
    expect(pathOf('http://servdesk.example:8080/api/tickets/7/actions/resolve')).toBe(
      '/api/tickets/7/actions/resolve',
    );
    expect(pathOf('/api/incidents/7?x=1')).toBe('/api/incidents/7?x=1');
  });

  it('reads the offered actions from the action:* links, in the server’s order', () => {
    const actions = actionsOf({
      self: { href: '/api/incidents/7' },
      'action:start-work': { href: '/api/tickets/7/actions/start-work' },
      requester: { href: '/api/persons/2' },
      'action:cancel': { href: '/api/tickets/7/actions/cancel' },
    });

    expect(actions.map((action) => action.name)).toEqual(['start-work', 'cancel']);
  });

  it('offers nothing without links', () => {
    expect(actionsOf(undefined)).toEqual([]);
  });
});
