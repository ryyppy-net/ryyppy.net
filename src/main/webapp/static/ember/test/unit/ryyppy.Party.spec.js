describe('Party module', function () {
    it('Construction', function () {
        var party = Party.create();
        expect(party).not.toBeUndefined();
        expect(party.get('name')).toBe('');
    });
});