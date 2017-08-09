	it('should test <% TESTSTEP %>', done => {
		const body = JSON.parse(`
<% BODY %>`);

		superagent.<% METHOD %>(`<% ENDPOINT %>/<% PATH %>`)
			.type('<% MEDIATYPE %>')
			.set(<% HEADER %>)
			.send(body)
			.end((err, res) => {
				<% ASSERTIONS %>
				done();
		});
	});